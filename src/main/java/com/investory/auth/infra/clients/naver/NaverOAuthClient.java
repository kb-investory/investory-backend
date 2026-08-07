package com.investory.auth.infra.clients.naver;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.OAuthUserInfo;
import com.investory.auth.infra.clients.OAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class NaverOAuthClient implements OAuthClient {
    private final RestTemplate restTemplate;

    @Value("${oauth.naver.client-id}")
    private String clientId;
    @Value("${oauth.naver.client-secret}")
    private String clientSecret;
    @Value("${oauth.naver.redirect-uri}")
    private String redirectUri;
    @Value("${oauth.naver.authorize-uri}")
    private String authorizeUri;
    @Value("${oauth.naver.token-uri}")
    private String tokenUri;
    @Value("${oauth.naver.user-info-uri}")
    private String userInfoUri;

    // 이 클라이언트가 담당하는 소셜 로그인 제공자가 NAVER임을 알려준다.
    @Override
    public OAuthProviderType getProvider() {
        return OAuthProviderType.NAVER;
    }

    // 사용자를 네이버 로그인 동의 화면으로 보내기 위한 인가 요청 URL을 만든다.
    // 네이버는 state를 CSRF 방지용으로 "필수"로 취급하므로 반드시 쿼리스트링에 실어 보낸다.
    @Override
    public String getAuthorizeUrl(String state) {
        return authorizeUri + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;
    }

    // 인가 코드(code)를 네이버 토큰 서버에 보내 accessToken으로 교환한다.
    // 네이버는 이때 인가 요청에서 썼던 state와 동일한 값을 함께 보내야 정상적으로 검증/발급된다.
    @Override
    public String getAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("state", state);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(tokenUri, HttpMethod.POST, request, NaverTokenResponse.class);

        return response.getBody().getAccess_token();
    }

    // accessToken으로 네이버 사용자 정보(고유 id, 이메일, 닉네임)를 조회해 공통 도메인 모델로 변환한다.
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<NaverUserResponse> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, NaverUserResponse.class);

        NaverUserResponse body = response.getBody();
        NaverUserResponse.Response naverProfile = body.getResponse();

        return OAuthUserInfo
                .builder()
                .oauthSubId(naverProfile.getId())
                .email(naverProfile.getEmail())
                .nickname(resolveNickname(naverProfile))
                .build();
    }

    // nickname -> name(실명) -> 없으면 최후 기본값 순으로 폴백해서, users.nickname의 NOT NULL 제약 위반으로 가입이 실패하지 않게 한다.
    private String resolveNickname(NaverUserResponse.Response naverProfile) {
        if (StringUtils.hasText(naverProfile.getNickname())) {
            return naverProfile.getNickname();
        }
        if (StringUtils.hasText(naverProfile.getName())) {
            return naverProfile.getName();
        }
        return "네이버사용자";
    }
}
