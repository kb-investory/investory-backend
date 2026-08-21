package com.investory.auth.infra.security;

import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.ports.dto.TokenPair;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final Long USER_ID = 42L;

    private final JwtTokenProvider provider = newProvider(RsaTestKeys.PRIVATE_KEY_A, RsaTestKeys.PUBLIC_KEY_A, 3600, 1209600);

    @Test
    void 발급한_액세스_토큰은_검증을_통과한다() {
        String token = provider.createAccessToken(USER_ID);

        provider.validateToken(token);
        assertEquals(USER_ID, provider.getUserId(token));
        assertFalse(provider.isRefreshToken(token));
    }

    @Test
    void 토큰_페어의_refresh_토큰은_isRefreshToken이_true다() {
        TokenPair pair = provider.createTokenPair(USER_ID);

        assertFalse(provider.isRefreshToken(pair.accessToken()));
        assertTrue(provider.isRefreshToken(pair.refreshToken()));
        assertEquals(USER_ID, provider.getUserId(pair.refreshToken()));
    }

    @Test
    void 만료된_토큰을_검증하면_EXPIRED_TOKEN_예외가_발생한다() {
        JwtTokenProvider expiredProvider = newProvider(RsaTestKeys.PRIVATE_KEY_A, RsaTestKeys.PUBLIC_KEY_A, -1, -1);
        String token = expiredProvider.createAccessToken(USER_ID);

        AuthException exception = assertThrows(AuthException.class, () -> expiredProvider.validateToken(token));

        assertEquals(AuthErrorCode.EXPIRED_TOKEN, exception.getErrorCode());
    }

    @Test
    void 다른_키로_서명된_토큰을_검증하면_INVALID_TOKEN_예외가_발생한다() {
        JwtTokenProvider otherProvider = newProvider(RsaTestKeys.PRIVATE_KEY_B, RsaTestKeys.PUBLIC_KEY_B, 3600, 1209600);
        String tokenSignedByOther = otherProvider.createAccessToken(USER_ID);

        AuthException exception = assertThrows(AuthException.class, () -> provider.validateToken(tokenSignedByOther));

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void 위조된_토큰을_검증하면_INVALID_TOKEN_예외가_발생한다() {
        String token = provider.createAccessToken(USER_ID);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        AuthException exception = assertThrows(AuthException.class, () -> provider.validateToken(tampered));

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void getPublicKey는_RSA_공개키를_반환한다() {
        assertNotNull(provider.getPublicKey());
        assertEquals("RSA", provider.getPublicKey().getAlgorithm());
    }

    // JwksController가 독립적으로 계산하는 kid와 반드시 일치해야 클라이언트가 JWKS에서 검증 키를
    // 찾을 수 있다 — idFromThumbprint()의 결정론적 특성(RFC 7638)에 의존하는 계약이므로 회귀 방지용.
    @Test
    void 서명된_토큰_헤더의_kid는_공개키_thumbprint와_일치한다() {
        String token = provider.createAccessToken(USER_ID);
        String kidFromToken = Jwts.parser().verifyWith(provider.getPublicKey()).build()
                .parseSignedClaims(token).getHeader().getKeyId();

        String kidFromPublicKey = Jwks.builder().key(provider.getPublicKey()).idFromThumbprint().build().getId();

        assertEquals(kidFromPublicKey, kidFromToken);
    }

    private static JwtTokenProvider newProvider(String privateKeyBase64, String publicKeyBase64,
                                                 long accessTokenExpireSeconds, long refreshTokenExpireSeconds) {
        return new JwtTokenProvider(privateKeyBase64, publicKeyBase64, accessTokenExpireSeconds, refreshTokenExpireSeconds);
    }
}
