package com.investory.auth.domain.ports;

import com.investory.auth.domain.ports.dto.TokenPair;

public interface TokenProvider {

    // Token Pair(Access Token 과 Refresh Token)를 발급하는 Method
    TokenPair createTokenPair(Long userId);

    // Refresh Token 으로 Access Token 을 발급하는 Method
    String createAccessToken(Long userId);

    long getAccessTokenExpireSeconds();

    // Token 이 Refresh Token 인지 확인하는 Method
    boolean isRefreshToken(String token);

    // Token 의 유효성을 검사하는 Method
    void validateToken(String token);

    // Access Token 으로 UserId 를 조회하는 Method
    Long getUserId(String token);
}
