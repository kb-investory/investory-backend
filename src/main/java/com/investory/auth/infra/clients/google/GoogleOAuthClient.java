package com.investory.auth.infra.clients.google;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.OAuthUserInfo;
import com.investory.auth.infra.clients.OAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthClient {
    private final RestTemplate restTemplate;

    @Value("${oauth.google.client-id}")
    private String clientId;
    @Value("${oauth.google.client-secret}")
    private String clientSecret;
    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;
    @Value("${oauth.google.authorize-uri}")
    private String authorizeUri;
    @Value("${oauth.google.token-uri}")
    private String tokenUri;
    @Value("${oauth.google.user-info-uri}")
    private String userInfoUri;

    @Override
    public OAuthProviderType getProvider() {
        return OAuthProviderType.GOOGLE;
    }

    // 사용자를 구글 로그인 동의 화면으로 보내기 위한 인가 요청 URL을 만든다.
    // 구글도 state를 필수로 검증하지 않으므로 이 구현체는 파라미터를 받되 사용하지 않는다.
    // scope는 구글에서 필수 파라미터라, 없으면 "Missing required parameter: scope" 에러가 난다.
    @Override
    public String getAuthorizeUrl(String state) {
        return authorizeUri + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=" + "openid%20email%20profile";
    }

    // 인가 코드(code)를 구글 토큰 서버에 보내 accessToken으로 교환한다. (state는 구글 토큰 교환에 불필요해 사용하지 않음)
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

        ResponseEntity<GoogleTokenResponse> response =
                restTemplate.exchange(tokenUri, HttpMethod.POST, request, GoogleTokenResponse.class);

        return response.getBody().getAccess_token();
    }

    // accessToken으로 구글 사용자 정보(sub, 이메일, 이름)를 조회해 공통 도메인 모델로 변환한다.
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserResponse> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, GoogleUserResponse.class);

        GoogleUserResponse body = response.getBody();

        return OAuthUserInfo
                .builder()
                .oauthSubId(body.getSub())
                .email(body.getEmail())
                .nickname(body.getName())
                .build();
    }
}

