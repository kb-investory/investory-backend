package com.investory.auth.presentation.controller;

import com.investory.auth.infra.security.JwtTokenProvider;
import com.investory.auth.infra.security.RsaTestKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwksControllerTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            RsaTestKeys.PRIVATE_KEY_A, RsaTestKeys.PUBLIC_KEY_A, 3600, 1209600);
    private final JwksController controller = new JwksController(tokenProvider);

    @Test
    void RSA_공개키_하나를_JWK_형식으로_반환한다() {
        Map<String, Object> response = controller.getJwks();

        assertTrue(response.get("keys") instanceof List<?>);
        List<?> keys = (List<?>) response.get("keys");
        assertEquals(1, keys.size());
    }

    @Test
    void JWK에_담긴_kid는_발급된_토큰_헤더의_kid와_일치한다() {
        Map<String, Object> response = controller.getJwks();
        List<?> keys = (List<?>) response.get("keys");
        io.jsonwebtoken.security.Jwk<?> jwk = (io.jsonwebtoken.security.Jwk<?>) keys.get(0);

        String token = tokenProvider.createAccessToken(1L);
        String kidFromToken = Jwts.parser().verifyWith(tokenProvider.getPublicKey()).build()
                .parseSignedClaims(token).getHeader().getKeyId();

        assertNotNull(jwk.getId());
        assertEquals(kidFromToken, jwk.getId());
    }
}
