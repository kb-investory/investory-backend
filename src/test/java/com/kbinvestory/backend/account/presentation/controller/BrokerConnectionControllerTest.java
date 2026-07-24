package com.kbinvestory.backend.account.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbinvestory.backend.account.domain.model.BrokerageProviderFixture;
import com.kbinvestory.backend.account.domain.ports.dto.BrokerAuthInfo;
import com.kbinvestory.backend.account.domain.repositories.FakeAccountConnectionRepository;
import com.kbinvestory.backend.account.domain.repositories.FakeBrokerageProviderRepository;
import com.kbinvestory.backend.account.domain.services.AccountConnectionService;
import com.kbinvestory.backend.account.presentation.dto.response.BrokerConnectionResponse;
import com.kbinvestory.backend.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerConnectionControllerTest {

    private FakeBrokerageProviderRepository brokerageProviderRepository;
    private FakeAccountConnectionRepository accountConnectionRepository;

    @BeforeEach
    void setUp() {
        brokerageProviderRepository = new FakeBrokerageProviderRepository();
        brokerageProviderRepository.add(BrokerageProviderFixture.provider(1L, "KB", "KB증권", true));
        accountConnectionRepository = new FakeAccountConnectionRepository();
    }

    @Test
    void 인증에_성공하면_201과_연동_결과를_반환한다() throws Exception {
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(true, "CONNECTED_ID_1", null));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerConnectionController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = mockMvc.perform(post("/broker-connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":100,\"providerId\":1,\"loginId\":\"myid\",\"password\":\"mypw\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        BrokerConnectionResponse response = new ObjectMapper()
                .readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), BrokerConnectionResponse.class);
        assertEquals(1L, response.connectionId());
        assertEquals("CONNECTED", response.status().name());
    }

    @Test
    void 존재하지_않는_증권사면_404를_반환한다() throws Exception {
        AccountConnectionService service = new AccountConnectionService(
                brokerageProviderRepository, accountConnectionRepository,
                (providerCode, loginId, password) -> new BrokerAuthInfo(true, "CONNECTED_ID_1", null));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerConnectionController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/broker-connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":100,\"providerId\":999,\"loginId\":\"myid\",\"password\":\"mypw\"}"))
                .andExpect(status().isNotFound());
    }
}
