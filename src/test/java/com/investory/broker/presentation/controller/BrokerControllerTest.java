package com.investory.broker.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.services.BrokerProviderService;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerControllerTest {

    @Test
    void 지원_증권사_목록을_반환한다() throws Exception {
        FakeBrokerProviderRepository repository = new FakeBrokerProviderRepository();
        repository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerProviderService service = new BrokerProviderService(repository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(service)).build();

        MvcResult result = mockMvc.perform(get("/broker/providers"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode provider = json.get("providers").get(0);

        assertEquals(1, json.get("providers").size());
        assertEquals(1, provider.get("brokerId").asLong());
        assertEquals("S9990001A", provider.get("brokerCode").asText());
        assertEquals("미래에셋증권(모의)", provider.get("brokerName").asText());
    }

    @Test
    void 인프라_예외는_GlobalExceptionHandler_응답_포맷으로_변환된다() throws Exception {
        BrokerProviderService service = new BrokerProviderService(
                () -> { throw new BrokerInfraException(new RuntimeException("DB down")); }
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = mockMvc.perform(get("/broker/providers"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("INTERNAL_ERROR", json.get("errorCode").asText());
        assertTrue(json.get("fieldErrors") == null || json.get("fieldErrors").isEmpty());
    }
}