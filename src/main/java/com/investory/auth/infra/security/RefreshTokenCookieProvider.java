package com.investory.auth.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";

    @Value("${jwt.refresh-token-expire-seconds}")
    private long refreshTokenExpireSeconds;

    // 로컬 개발환경(HTTP)에서는 false, 배포환경(HTTPS)에서는 반드시 true로 바꿀 것
    @Value("${app.cookie.secure:false}")
    private boolean secure;

    // Refresh Token 을 Response Cookie 로 만들어주는 Method
    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTokenExpireSeconds))
                .build();
    }

    // Refresh Token 을 삭제하기 위한 Response Cookie 를 만들어주는 Method
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

