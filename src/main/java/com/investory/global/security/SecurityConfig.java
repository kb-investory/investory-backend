package com.investory.global.security;

import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // OAuth 로그인/토큰 재발급/로그아웃은 토큰이 없는 상태에서 호출되므로 인증 없이 허용.
    // market 도메인(증권/시세 조회)은 유저 무관 공개 데이터라 permitAll.
    // swagger 쪽은 springfox 정적 리소스/문서 엔드포인트라 permitAll.
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
            "/markets/securities/**",
            "/market/securities/**",
            "/swagger-ui/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/webjars/**"
    };

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173"
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
        configuration.setAllowedOrigins(List.of(ALLOWED_ORIGINS));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 쿠키(accessToken/refreshToken)를 실어 보내려면 자격 증명 허용이 필수이며,
        // 이 경우 AllowedOrigins에 "*"를 쓸 수 없고 명시적인 origin만 등록해야 한다.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(toAntMatchers(PERMIT_ALL_PATHS)).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
