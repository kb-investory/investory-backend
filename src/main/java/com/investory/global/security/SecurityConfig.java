package com.investory.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final String allowedOriginsRaw;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            @Value("${app.allowed-origins}") String allowedOriginsRaw
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.allowedOriginsRaw = allowedOriginsRaw;
    }

    // OAuth 로그인/토큰 재발급/로그아웃은 토큰이 없는 상태에서 호출되므로 인증 없이 허용.
    // JWKS(/.well-known/jwks.json)는 investory-simulation-api 등 다른 서비스가 이 서버가 발급한
    // JWT를 검증할 때 쓰는 공개키라 인증과 무관하게 공개돼야 한다.
    // market 도메인(증권/시세 조회)은 유저 무관 공개 데이터라 permitAll.
    // swagger 쪽은 정적 리소스/문서 엔드포인트라 permitAll.
    // /health는 GCP LB 헬스체크가 인증 없이 찌르므로 permitAll.
    private static final String[] PERMIT_ALL_PATHS = {
            "/auth/oauth/kakao/authorization",
            "/auth/oauth/kakao/callback",
            "/auth/oauth/naver/authorization",
            "/auth/oauth/naver/callback",
            "/auth/oauth/google/authorization",
            "/auth/oauth/google/callback",
            "/auth/token/refresh",
            "/auth/logout",
            "/auth/me",
            "/.well-known/jwks.json",
            "/health",
            "/markets/securities/**",
            "/market/securities/**",
            "/swagger-ui/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/webjars/**"
    };

    private AntPathRequestMatcher[] toAntMatchers(String[] paths) {
        AntPathRequestMatcher[] matchers = new AntPathRequestMatcher[paths.length];

        for (int i = 0; i < paths.length; i++) {
            matchers[i] = new AntPathRequestMatcher(paths[i]);
        }

        return matchers;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 쉼표로 구분된 여러 origin을 각각 분리
        List<String> allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(List.of("*"));

        // accessToken / refreshToken 쿠키 전송 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 미등록 시 Spring Security 기본값(Http403ForbiddenEntryPoint)으로 떨어져, 토큰 없음/
                // 만료/위조가 전부 본문 없는 403으로 응답되고 GlobalExceptionHandler의 공통 ErrorResponse
                // 포맷을 못 탄다 — JwtAuthenticationEntryPoint로 401 + 기존 ErrorResponse 포맷을 보장한다.
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(toAntMatchers(PERMIT_ALL_PATHS))
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}