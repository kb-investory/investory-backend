package com.investory.auth.infra.security;

import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.ports.TokenProvider;
import com.investory.auth.domain.ports.dto.TokenPair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

// investory-simulation-api 등 다른 서비스가 이 백엔드가 발급한 토큰을 검증만 할 수 있어야 해서
// (발급까지 할 수 있게 하면 대칭키 유출 시 그쪽이 우리 사용자 토큰을 위조할 수 있음) HMAC 대신
// RS256(비대칭키)을 쓴다. 이 서버만 private key로 서명하고, public key는 JwksController가
// /auth/.well-known/jwks.json으로 공개한다.
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;
    private final long accessTokenExpireSeconds;
    private final long refreshTokenExpireSeconds;

    // jwt.private-key/jwt.public-key는 PKCS8/X509 DER 바이트를 base64 인코딩한 값이다
    // (PEM 헤더·개행 없이) — openssl로 만들 때: `openssl genpkey -algorithm RSA
    // -pkeyopt rsa_keygen_bits:2048 -outform DER | base64 -w0`,
    // `openssl rsa -pubout -outform DER -in private.der -inform DER | base64 -w0`.
    public JwtTokenProvider(
            @Value("${jwt.private-key}") String privateKeyBase64,
            @Value("${jwt.public-key}") String publicKeyBase64,
            @Value("${jwt.access-token-expire-seconds}") long accessTokenExpireSeconds,
            @Value("${jwt.refresh-token-expire-seconds}") long refreshTokenExpireSeconds) {
        this.privateKey = parsePrivateKey(privateKeyBase64);
        this.publicKey = parsePublicKey(publicKeyBase64);
        // JWKS의 kid와 반드시 같은 값이어야 클라이언트(PyJWKClient 등)가 헤더의 kid로 검증 키를
        // 찾을 수 있다. idFromThumbprint()는 RFC 7638 규격대로 공개키 자체에서 결정론적으로
        // 계산되므로, JwksController가 별도로 같은 공개키에 대해 다시 계산해도 항상 같은 값이 나온다.
        this.keyId = Jwks.builder().key(publicKey).idFromThumbprint().build().getId();
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
        this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
    }

    @Override
    public TokenPair createTokenPair(Long userId) {
        return new TokenPair(createAccessToken(userId), createRefreshToken(userId));
    }

    @Override
    public String createAccessToken(Long userId) {
        return createToken(userId, ACCESS_TOKEN_TYPE, accessTokenExpireSeconds);
    }

    private String createRefreshToken(Long userId) {
        return createToken(userId, REFRESH_TOKEN_TYPE, refreshTokenExpireSeconds);
    }

    private String createToken(Long userId, String tokenType, long expireSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .header().keyId(keyId).and()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Override
    public long getAccessTokenExpireSeconds() {
        return accessTokenExpireSeconds;
    }

    // Token이 유효한지 확인하는 Method
    @Override
    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    // Token이 Refresh Token 인지 확인하는 Method
    @Override
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
            return REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    // Token에서 User ID 를 추출하는 메서드
    @Override
    public Long getUserId(String token) {
        try {
            return Long.valueOf(parseClaims(token).getSubject());
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    // JWT의 Payload를 Claims 객체로 변환하여 반환하는 Method
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static RSAPrivateKey parsePrivateKey(String base64Der) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Der);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("JWT private key를 로드하는 중 오류가 발생했습니다.", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String base64Der) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Der);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("JWT public key를 로드하는 중 오류가 발생했습니다.", e);
        }
    }
}
