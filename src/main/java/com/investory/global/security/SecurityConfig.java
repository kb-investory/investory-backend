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
public class SecurityConfig {

    // TODO: JWT 인증 필터 도입 전까지는 전체 허용 상태로 기동 확인만 한다.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

// 실제 서비스시 설정 변경 후 확인
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    private static final String[] PERMIT_ALL_PATHS = {
//            "/auth/oauth/kakao/authorization",
//            "/auth/oauth/kakao/callback",
//            "/auth/oauth/naver/authorization",
//            "/auth/oauth/naver/callback",
//            "/auth/oauth/google/authorization",
//            "/auth/oauth/google/callback",
//            "/auth/token/refresh",
//            "/auth/logout",
//            "/auth/oauth/NAVER/authorization",
//            "/auth/me"
//    };
//
//    private static final String[] ALLOWED_ORIGINS = {
//            "http://localhost:5173"
//    };
//
//    private AntPathRequestMatcher[] toAntMatchers(String[] paths) {
//        AntPathRequestMatcher[] matchers = new AntPathRequestMatcher[paths.length];
//        for (int i = 0; i < paths.length; i++) {
//            matchers[i] = new AntPathRequestMatcher(paths[i]);
//        }
//        return matchers;
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(List.of(ALLOWED_ORIGINS));
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("*"));
//        // 쿠키(accessToken/refreshToken)를 실어 보내려면 자격 증명 허용이 필수이며,
//        // 이 경우 AllowedOrigins에 "*"를 쓸 수 없고 명시적인 origin만 등록해야 한다.
//        configuration.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(toAntMatchers(PERMIT_ALL_PATHS)).permitAll()
//                        .anyRequest().authenticated()
//                )
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//        return http.build();
//    }
//}
