package com.investory.broker.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.ports.FakeHoldingIngestionPort;
import com.investory.broker.domain.ports.FakeTradeIngestionPort;
import com.investory.broker.domain.repositories.FakeAccountSyncBatchRepository;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.BrokerConnectionService;
import com.investory.broker.domain.services.BrokerProviderService;
import com.investory.broker.infra.clients.FakeBrokerDataClient;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerControllerTest {

    private static BrokerConnectionService newConnectionService(FakeBrokerConnectionRepository repository) {
        return newConnectionService(repository, new FakeBrokerProviderRepository());
    }

    private static BrokerConnectionService newConnectionService(
            FakeBrokerConnectionRepository repository, FakeBrokerProviderRepository providerRepository) {
        return new BrokerConnectionService(
                repository,
                providerRepository,
                new FakeInvestmentAccountRepository(),
                new FakeAccountSyncBatchRepository(),
                new FakeTradeIngestionPort(),
                new FakeHoldingIngestionPort(),
                new FakeBrokerDataClient()
        );
    }

    @Test
    void 지원_증권사_목록을_반환한다() throws Exception {
        FakeBrokerProviderRepository repository = new FakeBrokerProviderRepository();
        repository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerProviderService providerService = new BrokerProviderService(repository);
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService)).build();

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
        BrokerProviderService providerService = new BrokerProviderService(new FailingBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService))
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

    @Test
    void 연동된_증권사_목록을_반환한다() throws Exception {
        BrokerProviderService providerService = new BrokerProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository repository = new FakeBrokerConnectionRepository();
        repository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerConnectionService connectionService = newConnectionService(repository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService)).build();

        MvcResult result = mockMvc.perform(get("/broker/connections"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode connection = json.get("connections").get(0);

        assertEquals(1, json.get("connections").size());
        assertEquals(15, connection.get("connectionId").asLong());
        assertEquals("S9990001A", connection.get("brokerCode").asText());
        assertEquals("CONNECTED", connection.get("connectionStatus").asText());
        assertEquals(2, connection.get("accountCount").asInt());
    }

    @Test
    void 연동한_증권사가_없으면_빈_배열을_반환한다() throws Exception {
        BrokerProviderService providerService = new BrokerProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService)).build();

        MvcResult result = mockMvc.perform(get("/broker/connections"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertTrue(json.get("connections").isEmpty());
    }

    @Test
    void 증권사_연동_요청이_성공하면_201과_연동결과를_반환한다() throws Exception {
        FakeBrokerProviderRepository providerRepository = new FakeBrokerProviderRepository();
        providerRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerProviderService providerService = new BrokerProviderService(providerRepository);
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository(), providerRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService)).build();

        MvcResult result = mockMvc.perform(post("/broker/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brokerId\":1,\"loginId\":\"demo1\",\"password\":\"1234\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("CONNECTED", json.get("connectionStatus").asText());
        assertEquals("S9990001A", json.get("brokerCode").asText());
        assertEquals("SUCCESS", json.get("syncResult").get("syncStatus").asText());
    }

    @Test
    void 존재하지_않는_브로커로_연동_요청하면_404를_반환한다() throws Exception {
        BrokerProviderService providerService = new BrokerProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = mockMvc.perform(post("/broker/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brokerId\":999,\"loginId\":\"demo1\",\"password\":\"1234\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("BRK_001", json.get("errorCode").asText());
    }

    private static class FailingBrokerProviderRepository implements com.investory.broker.domain.repositories.BrokerProviderRepository {
        @Override
        public java.util.List<com.investory.broker.domain.model.BrokerProvider> findAllActive() {
            throw new BrokerInfraException(new RuntimeException("DB down"));
        }

        @Override
        public java.util.Optional<com.investory.broker.domain.model.BrokerProvider> findById(Long brokerId) {
            throw new BrokerInfraException(new RuntimeException("DB down"));
        }
    }
}
