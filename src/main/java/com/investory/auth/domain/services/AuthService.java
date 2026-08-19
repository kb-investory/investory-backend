package com.investory.auth.domain.services;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.constant.UserStatusType;
import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.model.OAuthUserInfo;
import com.investory.auth.domain.model.User;
import com.investory.auth.domain.ports.BrokerConnectionCleanupPort;
import com.investory.auth.domain.ports.JournalCleanupPort;
import com.investory.auth.domain.ports.PrincipleCleanupPort;
import com.investory.auth.domain.ports.TokenProvider;
import com.investory.auth.domain.ports.dto.TokenPair;
import com.investory.auth.domain.repositories.UserRepository;
import com.investory.auth.domain.services.dto.command.OAuthLoginCommand;
import com.investory.auth.domain.services.dto.command.ReissueTokenCommand;
import com.investory.auth.domain.services.dto.query.GetUserQuery;
import com.investory.auth.domain.services.dto.result.LoginResult;
import com.investory.auth.domain.services.dto.result.ReissueResult;
import com.investory.auth.domain.services.dto.result.UserResult;
import com.investory.auth.infra.clients.OAuthClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final Map<OAuthProviderType, OAuthClient> oAuthClients;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final BrokerConnectionCleanupPort brokerConnectionCleanupPort;
    private final JournalCleanupPort journalCleanupPort;
    private final PrincipleCleanupPort principleCleanupPort;

    // provider별 OAuthClient 빈들을 Map으로 미리 캐싱해둔다 (나중에 NAVER/GOOGLE 클라이언트를 추가해도 이 코드는 안 바뀜)
    public AuthService(List<OAuthClient> oAuthClients, UserRepository userRepository, TokenProvider tokenProvider,
                        BrokerConnectionCleanupPort brokerConnectionCleanupPort, JournalCleanupPort journalCleanupPort,
                        PrincipleCleanupPort principleCleanupPort) {
        this.oAuthClients = oAuthClients.stream()
                .collect(Collectors.toMap(OAuthClient::getProvider, Function.identity()));
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.brokerConnectionCleanupPort = brokerConnectionCleanupPort;
        this.journalCleanupPort = journalCleanupPort;
        this.principleCleanupPort = principleCleanupPort;
    }

    // OAuth Login Logic
    public LoginResult login(OAuthLoginCommand command) {
        // KAKAO, NAVER, GOOGLE 중 어떤 Provider 를 사용하는지 결정
        OAuthClient oAuthClient = resolveClient(command.oauthProvider());

        // Access Token 을 통해서 User Information 을 조회 (state는 provider가 필요로 할 때만 검증에 사용됨)
        String accessToken = oAuthClient.getAccessToken(command.code(), command.state());
        OAuthUserInfo oAuthUserInfo = oAuthClient.getUserInfo(accessToken);

        // 해당 User Information 에 맞는 사용자가 있는지 조회
        User user = userRepository.findByOauthProviderAndOauthSubId(command.oauthProvider(), oAuthUserInfo.getOauthSubId())
                .orElse(null);

        // 해당하는 사용자가 없다면 DB 에 저장
        boolean newUser = (user == null);
        if (newUser) {
            user = userRepository.save(User.create(command.oauthProvider(), oAuthUserInfo));
        }

        // 해당 사용자가 탈퇴 상태라면 예외 처리
        if (user.isWithdrawn()) {
            throw new AuthException(AuthErrorCode.WITHDRAWN_USER);
        }

        // 해당 정보로 Token Pair 발급
        TokenPair tokenPair = tokenProvider.createTokenPair(user.getUserId());

        return new LoginResult(user.getUserId(), tokenPair.accessToken(), tokenPair.refreshToken(), newUser);
    }

    // Refresh Token 으로 새로운 Access Token 발급 Logic
    // 리프레시 토큰의 서명/만료를 검증하고, refresh 타입이 맞는지 + 회원이 유효한지 재확인한 뒤 access 토큰만 새로 발급한다.
    public ReissueResult reissue(ReissueTokenCommand command) {
        String refreshToken = command.refreshToken();
        tokenProvider.validateToken(refreshToken);
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.NOT_REFRESH_TOKEN);
        }

        Long userId = tokenProvider.getUserId(refreshToken);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new AuthException(AuthErrorCode.WITHDRAWN_USER);
        }

        return new ReissueResult(tokenProvider.createAccessToken(user.getUserId()), tokenProvider.getAccessTokenExpireSeconds());
    }

    // 회원 정보 조회
    public UserResult getMe(GetUserQuery query) {
        User user = userRepository.findByUserId(query.userId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        return UserResult.from(user);
    }

    // 계정 탈퇴: 다른 도메인의 사용자 소유 데이터를 먼저 정리한 뒤 users 행을 WITHDRAWN으로 전이한다.
    // 이미 탈퇴한 회원이면 멱등적으로 아무 것도 하지 않는다. broker 정리(증권사 연동 해지)를 가장 먼저
    // 호출한다 — 그 결과로 ledger(거래/보유)와 journal의 매매 근거까지 함께 정리되므로, journal 정리는
    // 그 뒤에 investment_journals(자유 텍스트 일지)만 지우면 된다. notification 정리는 아직 없다
    // (feat/notification-domain 병합 후 추가 예정 — CLAUDE.md §8-1 참고).
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            return;
        }

        brokerConnectionCleanupPort.disconnectAllConnections(userId);
        journalCleanupPort.deleteAllJournals(userId);
        principleCleanupPort.deleteAllPrincipleSets(userId);

        User withdrawn = User.of(user.getUserId(), user.getOauthProvider(), user.getOauthSubId(), user.getEmail(),
                user.getNickname(), UserStatusType.WITHDRAWN, user.getCreatedAt(), LocalDateTime.now(), LocalDateTime.now());
        userRepository.save(withdrawn);
    }

    // 요청받은 provider(KAKAO/NAVER/GOOGLE)에 해당하는 OAuthClient를 찾고, 없으면 예외를 던진다.
    private OAuthClient resolveClient(OAuthProviderType provider) {
        OAuthClient client = oAuthClients.get(provider);
        if (client == null) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
        return client;
    }
}
