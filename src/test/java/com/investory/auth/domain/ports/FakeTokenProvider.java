package com.investory.auth.domain.ports;

import com.investory.auth.domain.ports.dto.TokenPair;

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
}
