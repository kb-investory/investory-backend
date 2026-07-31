package com.investory.auth.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 네이버처럼 authorize -> callback 두 요청 사이에서 state 값을 검증해야 하는 provider를 위한 유틸.
 * authorize 요청 시 발급한 state를 짧은 만료시간의 httpOnly 쿠키에 담아두고, callback에서 꺼내 비교한다.
 */
@Component
public class OAuthStateCookieProvider {

    public static final String COOKIE_NAME = "oauth_state";
    private static final long MAX_AGE_SECONDS = 300; // 5분 - 로그인 흐름 왕복 시간이면 충분

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    // CSRF 방지용으로 사용할 임의의 state 문자열을 새로 만든다.
    public String generateState() {
        return UUID.randomUUID().toString();
    }

    // 방금 발급한 state를 authorize 응답에 실어 보낼 httpOnly 쿠키로 만든다.
    public ResponseCookie create(String state) {
        return ResponseCookie.from(COOKIE_NAME, state)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(MAX_AGE_SECONDS))
                .build();
    }

    // 콜백 검증이 끝나면 더 이상 필요 없으므로 즉시 만료시켜 지운다.
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
