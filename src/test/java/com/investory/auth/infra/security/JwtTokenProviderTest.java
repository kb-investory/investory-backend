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

    // signature 세그먼트의 "마지막" 문자를 변조하면 base64url 인코딩 특성상 결정론적으로 실패할 수
    // 있다 — RSA-2048 signature는 256바이트라 256 mod 3 = 1, 마지막 문자는 원본 바이트의 상위 2비트만
    // 의미 있고 나머지 4비트는 항상 0인 패딩이라 디코더가 무시한다. 이전엔 이 마지막 문자를 'a'/'b'로
    // 바꿨는데, 두 문자(인덱스 26/27, 상위 2비트 모두 01)가 우연히 같은 유효 비트를 가리켜서 실제로는
    // 아무것도 안 바뀌는 경우가 생겼다(#176). signature 세그먼트의 첫 번째 문자는 항상 완전한 6비트가
    // 의미 있는 데이터라 이런 패딩 비트 우연 일치가 구조적으로 발생하지 않는다.
    @Test
    void 위조된_토큰을_검증하면_INVALID_TOKEN_예외가_발생한다() {
        String token = provider.createAccessToken(USER_ID);
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        char replacement = original == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

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
