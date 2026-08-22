package com.investory.auth.infra.clients.kakao;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.OAuthUserInfo;
import com.investory.auth.infra.clients.OAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthClient implements OAuthClient {
    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id}")
    private String clientId;
    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;
    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;
    @Value("${oauth.kakao.authorize-uri}")
    private String authorizeUri;
    @Value("${oauth.kakao.token-uri}")
    private String tokenUri;
    @Value("${oauth.kakao.user-info-uri}")
    private String userInfoUri;

    @Override
    public OAuthProviderType getProvider() {
        return OAuthProviderType.KAKAO;
    }

    // 사용자를 카카오 로그인 동의 화면으로 보내기 위한 인가 요청 URL을 만든다.
    // 카카오는 state를 필수로 검증하지 않으므로 이 구현체는 파라미터를 받되 사용하지 않는다.
    @Override
    public String getAuthorizeUrl(String state) {
        log.debug("Kakao authorize request: redirect_uri={}", redirectUri);
        return authorizeUri + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri=" + redirectUri;
    }

    // 인가 코드(code)를 카카오 토큰 서버에 보내 accessToken으로 교환한다. (state는 카카오 토큰 교환에 불필요해 사용하지 않음)
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

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.debug("Kakao token exchange request: redirect_uri={}", redirectUri);
        ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(tokenUri, HttpMethod.POST, request, KakaoTokenResponse.class);

        KakaoTokenResponse body = response.getBody();
        String accessToken = body.getAccess_token();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.error("Kakao token exchange failed: access_token is null or empty. Response: {}", body);
            throw new RuntimeException("Kakao OAuth token exchange returned empty access token");
        }

        log.debug("Kakao token response: access_token_length={}", accessToken.length());

        return accessToken;
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, KakaoUserResponse.class);

        KakaoUserResponse body = response.getBody();

        return OAuthUserInfo
                .builder()
                .oauthSubId(body.getId().toString())
                .email(body.getKakao_account().getEmail())
                .nickname(body.getKakao_account().getProfile().getNickname()).build();
    }
}