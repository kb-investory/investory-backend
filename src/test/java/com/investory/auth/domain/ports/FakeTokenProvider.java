package com.investory.auth.domain.ports;

import com.investory.auth.domain.ports.dto.TokenPair;

import java.security.interfaces.RSAPublicKey;

public class FakeTokenProvider implements TokenProvider {

    @Override
    public TokenPair createTokenPair(Long userId) {
        return new TokenPair("access-" + userId, "refresh-" + userId);
    }

    @Override
    public String createAccessToken(Long userId) {
        return "access-" + userId;
    }

    @Override
    public long getAccessTokenExpireSeconds() {
        return 3600L;
    }

    @Override
    public boolean isRefreshToken(String token) {
        return token.startsWith("refresh-");
    }

    @Override
    public void validateToken(String token) {
    }

    @Override
    public Long getUserId(String token) {
        return Long.valueOf(token.substring(token.indexOf('-') + 1));
    }

    @Override
    public RSAPublicKey getPublicKey() {
        // AuthServiceTest 시나리오 어디서도 JWKS 노출을 검증하지 않아 실제 키가 필요 없다.
        return null;
    }
}
