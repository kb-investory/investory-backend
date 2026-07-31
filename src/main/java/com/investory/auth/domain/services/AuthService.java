package com.investory.auth.domain.services;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.model.OAuthUserInfo;
import com.investory.auth.domain.model.User;
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final Map<OAuthProviderType, OAuthClient> oAuthClients;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    // provider별 OAuthClient 빈들을 Map으로 미리 캐싱해둔다 (나중에 NAVER/GOOGLE 클라이언트를 추가해도 이 코드는 안 바뀜)
    public AuthService(List<OAuthClient> oAuthClients, UserRepository userRepository, TokenProvider tokenProvider) {
        this.oAuthClients = oAuthClients.stream()
                .collect(Collectors.toMap(OAuthClient::getProvider, Function.identity()));
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
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

    // 요청받은 provider(KAKAO/NAVER/GOOGLE)에 해당하는 OAuthClient를 찾고, 없으면 예외를 던진다.
    private OAuthClient resolveClient(OAuthProviderType provider) {
        OAuthClient client = oAuthClients.get(provider);
        if (client == null) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
        return client;
    }
}
