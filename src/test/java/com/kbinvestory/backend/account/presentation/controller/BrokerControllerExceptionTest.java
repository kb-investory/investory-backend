package com.kbinvestory.backend.account.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbinvestory.backend.account.domain.services.BrokerService;
import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;
import com.kbinvestory.backend.global.error.ErrorResponse;
import com.kbinvestory.backend.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerControllerExceptionTest {

    @Test
    void 인프라_예외는_GlobalExceptionHandler_응답_포맷으로_변환된다() throws Exception {
        BrokerService brokerService = new BrokerService(query -> {
            throw new AccountInfraException(AccountInfraErrorCode.BROKERAGE_PROVIDER_QUERY_FAILED,
                    new RuntimeException("DB down"));
        });
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(brokerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = mockMvc.perform(get("/brokers"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        ErrorResponse response = new ObjectMapper()
                .readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), ErrorResponse.class);

        assertEquals("ACNT_INFRA_001", response.code());
        assertEquals("증권사 목록을 조회하는 중 오류가 발생했습니다.", response.message());
        assertTrue(response.fieldErrors().isEmpty());
    }
}
