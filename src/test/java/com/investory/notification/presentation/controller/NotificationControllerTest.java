package com.investory.notification.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.global.error.GlobalExceptionHandler;
import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.model.Notification;
import com.investory.notification.domain.repositories.FakeNotificationRepository;
import com.investory.notification.domain.repositories.FakeNotificationSettingsRepository;
import com.investory.notification.domain.services.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private static final Long TEMP_USER_ID = 1L;

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
    void 목록_조회하면_content와_unreadCount를_반환한다() throws Exception {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, TEMP_USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        MockMvc mockMvc = mockMvc(repository);

        MvcResult result = mockMvc.perform(get("/notifications").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(1, json.get("content").size());
        assertEquals(1, json.get("unreadCount").asInt());
    }

    @Test
    void 읽음처리하면_isRead가_true로_바뀐_응답을_반환한다() throws Exception {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, TEMP_USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        MockMvc mockMvc = mockMvc(repository);

        MvcResult result = mockMvc.perform(patch("/notifications/{notificationId}/read", 1L))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(true, json.get("isRead").asBoolean());
    }

    @Test
    void 존재하지_않는_알림을_읽음처리하면_404와_NOTI_001을_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeNotificationRepository());

        MvcResult result = mockMvc.perform(patch("/notifications/{notificationId}/read", 999L))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("NOTI_001", json.get("errorCode").asText());
    }

    @Test
    void 남의_알림을_읽음처리하면_404를_반환한다() throws Exception {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, 999L, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        MockMvc mockMvc = mockMvc(repository);

        mockMvc.perform(patch("/notifications/{notificationId}/read", 1L))
                .andExpect(status().isNotFound());
    }

    private MockMvc mockMvc(FakeNotificationRepository repository) {
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());
        return MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
