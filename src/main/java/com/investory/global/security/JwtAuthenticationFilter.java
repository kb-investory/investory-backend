package com.investory.global.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.ports.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer {accessToken} 헤더 또는 accessToken 쿠키를 검증하여 SecurityContext에 인증 정보를 채워 넣는다.
 * 헤더가 없으면 accessToken 쿠키를 폴백으로 사용한다.
 * 인증 정보의 principal은 User.userId(Long)이다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    // JwtAuthenticationEntryPoint가 이 값을 읽어 토큰 없음/만료/위조를 구분한 401 응답을 만든다.
    public static final String AUTH_ERROR_CODE_ATTRIBUTE = "com.investory.global.security.AUTH_ERROR_CODE";

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                tokenProvider.validateToken(token);
                if (!tokenProvider.isRefreshToken(token)) {
                    Long userId = tokenProvider.getUserId(token);
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (AuthException e) {
                SecurityContextHolder.clearContext();
                // 여기서 응답을 직접 만들지 않는다 — 인증 없이 필터체인을 계속 진행시키면
                // 보호된 경로에서 AuthorizationFilter가 인증 실패로 판단해 JwtAuthenticationEntryPoint를
                // 호출하는데, 그때 이 값을 읽어 토큰 없음(기본값)과 구분되는 정확한 401 응답을 만든다.
                request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, e.getErrorCode());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return resolveTokenFromCookie(request);
    }

    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
