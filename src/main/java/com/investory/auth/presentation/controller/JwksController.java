package com.investory.auth.presentation.controller;

import com.investory.auth.domain.ports.TokenProvider;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// investory-simulation-api 등 다른 서비스가 이 백엔드가 발급한 JWT(RS256)를 검증할 때 쓰는
// public key를 노출한다. SecurityConfig의 permitAll 대상 — 인증 없이 열려 있어야 한다.
// kid는 JwtTokenProvider가 서명 시 헤더에 붙이는 값과 항상 같다 — idFromThumbprint()가
// 같은 공개키에 대해 결정론적으로(RFC 7638) 계산하기 때문에 여기서 별도로 다시 계산해도 일치한다.
@RestController
public class JwksController {

    private final TokenProvider tokenProvider;

    public JwksController(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        PublicJwk<?> jwk = Jwks.builder()
                .key(tokenProvider.getPublicKey())
                .idFromThumbprint()
                .build();
        return Map.of("keys", List.of(jwk));
    }
}
