package com.investory.notification.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.global.error.GlobalExceptionHandler;
import com.investory.notification.domain.repositories.FakeNotificationSettingsRepository;
import com.investory.notification.domain.services.NotificationSettingsService;
import com.investory.notification.presentation.dto.request.UpdateNotificationSettingsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationSettingControllerTest {

    private static final Long TEMP_USER_ID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEMP_USER_ID, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 설정_행이_없어도_기본값_전부_수신을_200으로_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeNotificationSettingsRepository());

        MvcResult result = mockMvc.perform(get("/users/me/notification-settings"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(true, json.get("tradeIngestedEnabled").asBoolean());
        assertEquals(true, json.get("tendencyAnalyzedEnabled").asBoolean());
        assertEquals(true, json.get("simulationCompletedEnabled").asBoolean());
    }

    @Test
    void 설정을_변경하면_저장된_값을_그대로_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeNotificationSettingsRepository());
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest(true, false, false);

        MvcResult result = mockMvc.perform(put("/users/me/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(true, json.get("tradeIngestedEnabled").asBoolean());
        assertEquals(false, json.get("tendencyAnalyzedEnabled").asBoolean());
        assertEquals(false, json.get("simulationCompletedEnabled").asBoolean());
    }

    @Test
    void 필드가_누락된_요청은_400과_NOTI_002를_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeNotificationSettingsRepository());
        String requestBody = "{\"tradeIngestedEnabled\": true, \"tendencyAnalyzedEnabled\": false}";

        MvcResult result = mockMvc.perform(put("/users/me/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("NOTI_002", json.get("errorCode").asText());
    }

    private MockMvc mockMvc(FakeNotificationSettingsRepository repository) {
        NotificationSettingsService service = new NotificationSettingsService(repository);
        return MockMvcBuilders.standaloneSetup(new NotificationSettingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
