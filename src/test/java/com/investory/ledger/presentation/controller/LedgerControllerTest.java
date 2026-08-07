package com.investory.ledger.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.global.error.GlobalExceptionHandler;
import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.TradeFixture;
import com.investory.ledger.domain.ports.FakeAccountPort;
import com.investory.ledger.domain.ports.FakeMarketDataPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.FakeHoldingSnapshotRepository;
import com.investory.ledger.domain.repositories.FakeTradeRepository;
import com.investory.ledger.domain.repositories.TradeSearchCriteria;
import com.investory.ledger.domain.services.HoldingQueryService;
import com.investory.ledger.domain.services.TradeQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 11L;
    private static final Long SECURITY_ID = 101L;

    @Test
    void 거래_목록을_조회하면_200과_함께_목록을_반환한다() throws Exception {
        FakeTradeRepository tradeRepository = new FakeTradeRepository();
        FakeAccountPort accountPort = new FakeAccountPort();
        FakeMarketDataPort marketDataPort = new FakeMarketDataPort();
        accountPort.add(USER_ID, new AccountInfo(ACCOUNT_ID, "국내주식계좌", "1234-****-5678", "한국투자증권"));
        marketDataPort.add(new SecurityInfo(SECURITY_ID, "005930", "삼성전자", "KOSPI", "반도체"));
        tradeRepository.add(TradeFixture.trade(ACCOUNT_ID, SECURITY_ID, TradeSide.BUY, "T-1", Instant.parse("2026-07-29T01:15:00Z")));

        MockMvc mockMvc = mockMvc(tradeRepository, new FakeHoldingSnapshotRepository(), accountPort, marketDataPort);

        MvcResult result = mockMvc.perform(get("/ledger/trades"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(1, json.get("content").size());
        assertEquals("삼성전자", json.get("content").get(0).get("securityName").asText());
        assertEquals("국내주식계좌", json.get("content").get(0).get("accountName").asText());
        assertEquals(1, json.get("totalElements").asInt());
        assertEquals(false, json.get("hasNext").asBoolean());
    }

    @Test
    void 거래_상세를_조회하면_200과_함께_상세정보를_반환한다() throws Exception {
        FakeTradeRepository tradeRepository = new FakeTradeRepository();
        FakeAccountPort accountPort = new FakeAccountPort();
        FakeMarketDataPort marketDataPort = new FakeMarketDataPort();
        accountPort.add(USER_ID, new AccountInfo(ACCOUNT_ID, "국내주식계좌", "1234-****-5678", "한국투자증권"));
        marketDataPort.add(new SecurityInfo(SECURITY_ID, "005930", "삼성전자", "KOSPI", "반도체"));
        tradeRepository.add(TradeFixture.trade(ACCOUNT_ID, SECURITY_ID, TradeSide.BUY, "T-1", Instant.parse("2026-07-29T01:15:00Z")));
        Long tradeId = tradeRepository.search(new TradeSearchCriteria(
                List.of(ACCOUNT_ID), null, null, null, null, 0, 10)).get(0).getTradeId();

        MockMvc mockMvc = mockMvc(tradeRepository, new FakeHoldingSnapshotRepository(), accountPort, marketDataPort);

        MvcResult result = mockMvc.perform(get("/ledger/trades/{tradeId}", tradeId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("한국투자증권", json.get("account").get("brokerageName").asText());
        assertEquals("KOSPI", json.get("security").get("marketType").asText());
    }

    @Test
    void 존재하지_않는_거래를_조회하면_404를_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeTradeRepository(), new FakeHoldingSnapshotRepository(), new FakeAccountPort(), new FakeMarketDataPort());

        MvcResult result = mockMvc.perform(get("/ledger/trades/{tradeId}", 999L))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("TRADE_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    void 조회_기간이_역전되면_400을_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeTradeRepository(), new FakeHoldingSnapshotRepository(), new FakeAccountPort(), new FakeMarketDataPort());

        MvcResult result = mockMvc.perform(get("/ledger/trades")
                        .param("from", "2026-07-30")
                        .param("to", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("LEDGER_INVALID_DATE_RANGE", json.get("errorCode").asText());
    }

    @Test
    void 보유종목이_없으면_200과_함께_빈_보유현황을_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeTradeRepository(), new FakeHoldingSnapshotRepository(), new FakeAccountPort(), new FakeMarketDataPort());

        MvcResult result = mockMvc.perform(get("/ledger/holdings"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(true, json.get("snapshotDate").isNull());
        assertEquals(0, json.get("holdings").size());
        assertEquals(0, json.get("summary").get("holdingCount").asInt());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MockMvc mockMvc(FakeTradeRepository tradeRepository, FakeHoldingSnapshotRepository holdingSnapshotRepository,
                             FakeAccountPort accountPort, FakeMarketDataPort marketDataPort) {
        TradeQueryService tradeQueryService = new TradeQueryService(tradeRepository, accountPort, marketDataPort);
        HoldingQueryService holdingQueryService = new HoldingQueryService(holdingSnapshotRepository, accountPort, marketDataPort);
        return MockMvcBuilders.standaloneSetup(new LedgerController(tradeQueryService, holdingQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
