package com.investory.auth.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * "/oauth/{provider}/authorization" 요청 시 쿼리파라미터로 받은 프론트 리다이렉트 목적지를
 * authorize -> callback 두 요청 사이에 보관했다가, 콜백에서 최종 리다이렉트할 때 사용한다.
 *
 * 쿼리파라미터 값을 검증 없이 그대로 리다이렉트에 쓰면 오픈 리다이렉트 취약점이 되므로,
 * app.frontend.allowed-redirect-origins에 등록된 origin으로 시작하는 값만 허용하고,
 * 그 외에는 기본 프론트 주소로 대체한다.
 */
@Component
public class PostLoginRedirectCookieProvider {

    public static final String COOKIE_NAME = "post_login_redirect";
    private static final long MAX_AGE_SECONDS = 300;

    @Value("${app.frontend.default-redirect-uri:http://localhost:5173}")
    private String defaultRedirectUri;

    @Value("${app.frontend.allowed-redirect-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    // 허용 목록에 있는 origin으로 시작하는 값만 통과시키고, 그 외에는 기본 주소로 강제 대체한다.
    public String sanitize(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return defaultRedirectUri;
        }
        List<String> allowedOrigins = Arrays.asList(allowedOriginsRaw.split(","));
        boolean allowed = allowedOrigins.stream()
                .map(String::trim)
                .anyMatch(redirectUri::startsWith);
        return allowed ? redirectUri : defaultRedirectUri;
    }

    // 검증된 리다이렉트 목적지를 authorize 응답에 실어 보낼 httpOnly 쿠키로 만든다.
    public ResponseCookie create(String sanitizedRedirectUri) {
        return ResponseCookie.from(COOKIE_NAME, sanitizedRedirectUri)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(MAX_AGE_SECONDS))
                .build();
    }

    // 콜백에서 다 쓰고 나면 더는 필요 없으므로 즉시 만료시켜 지운다.
    public ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
