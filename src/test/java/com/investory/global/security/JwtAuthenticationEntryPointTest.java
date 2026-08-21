package com.investory.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.auth.domain.ports.TokenProvider;
import com.investory.auth.infra.security.JwtTokenProvider;
import com.investory.auth.infra.security.RsaTestKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// standaloneSetup을 쓰는 다른 컨트롤러 테스트들과 달리, 여기서는 실제 SecurityConfig +
// JwtAuthenticationFilter + JwtAuthenticationEntryPoint를 전부 태우는 webAppContextSetup을 쓴다 —
// 그래야 "토큰 없음/만료/위조가 실제로 어떤 HTTP 응답을 만드는가"를 검증할 수 있다. RootConfig 전체를
// 로드하면 DB(DataSource/MyBatis)가 필요해지므로, 이 3개 클래스 + 목적에 맞는 최소 설정만 올린다.
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtAuthenticationEntryPointTest.TestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "app.allowed-origins=http://localhost:3000")
class JwtAuthenticationEntryPointTest {

    private static final Long USER_ID = 42L;
    private static final JwtTokenProvider VALID_PROVIDER =
            new JwtTokenProvider(RsaTestKeys.PRIVATE_KEY_A, RsaTestKeys.PUBLIC_KEY_A, 3600, 1209600);
    private static final JwtTokenProvider EXPIRED_PROVIDER =
            new JwtTokenProvider(RsaTestKeys.PRIVATE_KEY_A, RsaTestKeys.PUBLIC_KEY_A, -1, -1);
    private static final JwtTokenProvider OTHER_KEY_PROVIDER =
            new JwtTokenProvider(RsaTestKeys.PRIVATE_KEY_B, RsaTestKeys.PUBLIC_KEY_B, 3600, 1209600);

    private final MockMvc mockMvc;

    JwtAuthenticationEntryPointTest(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void 토큰_없이_보호된_API를_호출하면_401과_INVALID_TOKEN을_반환한다() throws Exception {
        MvcResult result = mockMvc.perform(get("/protected/ping"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertEquals("AUTH_007", readJson(result).get("errorCode").asText());
    }

    @Test
    void 만료된_토큰으로_호출하면_401과_EXPIRED_TOKEN을_반환한다() throws Exception {
        String expiredToken = EXPIRED_PROVIDER.createAccessToken(USER_ID);

        MvcResult result = mockMvc.perform(get("/protected/ping").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertEquals("AUTH_008", readJson(result).get("errorCode").asText());
    }

    @Test
    void 다른_키로_서명된_토큰으로_호출하면_401과_INVALID_TOKEN을_반환한다() throws Exception {
        String tokenSignedByOther = OTHER_KEY_PROVIDER.createAccessToken(USER_ID);

        MvcResult result = mockMvc.perform(get("/protected/ping").header("Authorization", "Bearer " + tokenSignedByOther))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertEquals("AUTH_007", readJson(result).get("errorCode").asText());
    }

    @Test
    void 유효한_토큰으로_호출하면_200을_반환한다() throws Exception {
        String token = VALID_PROVIDER.createAccessToken(USER_ID);

        mockMvc.perform(get("/protected/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void permitAll_경로는_토큰_없이도_200을_반환한다() throws Exception {
        mockMvc.perform(get("/market/securities/dummy"))
                .andExpect(status().isOk());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        public TokenProvider tokenProvider() {
            return VALID_PROVIDER;
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(TokenProvider tokenProvider) {
            return new JwtAuthenticationFilter(tokenProvider);
        }

        @Bean
        public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
            return new JwtAuthenticationEntryPoint();
        }

        // SecurityConfig.securityFilterChain(HttpSecurity)이 @Import(SecurityConfig.class)로 이미
        // @Bean 등록되므로 여기서 다시 선언하지 않는다.

        @Bean
        public DummyController dummyController() {
            return new DummyController();
        }
    }

    // TestConfig(@Configuration) 안에 중첩하면 Spring이 @Component 계열 static 멤버 클래스를
    // "lite configuration candidate"로 자동 처리해 @Bean 메서드와 별개로 한 번 더 등록해버려서
    // (ConfigurationClassParser.processMemberClasses) 핸들러 매핑이 중복 등록되는 문제가 있었다 —
    // @Configuration 클래스 밖으로 뺀다.
    @RestController
    static class DummyController {
        // permitAll 목록에 있는 실제 패턴(/market/securities/**)에 맞춰 "허용 목록은 그대로
        // 통과한다"를 같이 검증한다.
        @GetMapping("/market/securities/dummy")
        public String dummySecurity() {
            return "dummy";
        }

        @GetMapping("/protected/ping")
        public String ping() {
            return "pong";
        }
    }
}
