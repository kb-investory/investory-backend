package com.investory.journal.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.models.JournalFixture;
import com.investory.journal.domain.ports.FakeMarketDataPort;
import com.investory.journal.domain.ports.FakeTradeLedgerPort;
import com.investory.journal.domain.ports.dto.SecurityInfoFixture;
import com.investory.journal.domain.ports.dto.TradeTimelineInfoFixture;
import com.investory.journal.domain.repositories.FakeJournalRepository;
import com.investory.journal.domain.repositories.FakeJournalTradeNoteRepository;
import com.investory.journal.domain.services.JournalService;
import com.investory.journal.presentation.dto.request.CreateJournalRequest;
import com.investory.journal.presentation.dto.request.UpdateJournalRequest;
import com.investory.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JournalControllerTest {

    private static final Long TEMP_USER_ID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void 목록_조회하면_일지_목록을_반환한다() throws Exception {
        FakeJournalRepository journalRepository = new FakeJournalRepository();
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(1L, TEMP_USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));
        MockMvc mockMvc = mockMvc(journalRepository, new FakeJournalTradeNoteRepository(), new FakeTradeLedgerPort(), new FakeMarketDataPort());

        MvcResult result = mockMvc.perform(get("/journal/entries")
                        .param("startDate", journalDate.toString())
                        .param("endDate", journalDate.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(1, json.get("entries").size());
        assertEquals(1, json.get("entries").get(0).get("journalId").asLong());
    }

    @Test
    void 날짜별_상세_조회하면_해당_날짜의_일지를_반환한다() throws Exception {
        FakeJournalRepository journalRepository = new FakeJournalRepository();
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(1L, TEMP_USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));
        MockMvc mockMvc = mockMvc(journalRepository, new FakeJournalTradeNoteRepository(), new FakeTradeLedgerPort(), new FakeMarketDataPort());

        MvcResult result = mockMvc.perform(get("/journal/entries/on/{date}", journalDate))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(1, json.get("journal").get("journalId").asLong());
        assertTrue(json.get("trades").isEmpty());
    }

    @Test
    void journalId로_상세_조회하면_해당_일지를_반환한다() throws Exception {
        FakeJournalRepository journalRepository = new FakeJournalRepository();
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, TEMP_USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));
        MockMvc mockMvc = mockMvc(journalRepository, new FakeJournalTradeNoteRepository(), new FakeTradeLedgerPort(), new FakeMarketDataPort());

        MvcResult result = mockMvc.perform(get("/journal/entries/{journalId}", 305L))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(305, json.get("journal").get("journalId").asLong());
    }

    @Test
    void 저장하면_201과_함께_journalId를_반환한다() throws Exception {
        MockMvc mockMvc = mockMvc(new FakeJournalRepository(), new FakeJournalTradeNoteRepository(),
                new FakeTradeLedgerPort(), new FakeMarketDataPort());
        CreateJournalRequest request = new CreateJournalRequest(
                LocalDate.of(2020, 1, 1), "시장에 대한 생각", MarketMood.CALM, List.of());

        MvcResult result = mockMvc.perform(post("/journal/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = readJson(result);
        assertTrue(json.has("journalId"));
        assertTrue(json.has("createdAt"));
    }

    @Test
    void 수정하면_200과_함께_updatedAt을_반환한다() throws Exception {
        FakeJournalRepository journalRepository = new FakeJournalRepository();
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, TEMP_USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));
        MockMvc mockMvc = mockMvc(journalRepository, new FakeJournalTradeNoteRepository(), new FakeTradeLedgerPort(), new FakeMarketDataPort());
        UpdateJournalRequest request = new UpdateJournalRequest(
                "바뀐 생각", MarketMood.CONFIDENT, List.of());

        MvcResult result = mockMvc.perform(put("/journal/entries/{journalId}", 305L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals(305, json.get("journalId").asLong());
        assertTrue(json.has("updatedAt"));
    }

    @Test
    void 존재하지_않는_journalId를_조회하면_404와_JOURNAL_NOT_FOUND를_반환한다() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new JournalController(journalService(
                        new FakeJournalRepository(), new FakeJournalTradeNoteRepository(),
                        new FakeTradeLedgerPort(), new FakeMarketDataPort())))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = mockMvc.perform(get("/journal/entries/{journalId}", 999L))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("JNL_009", json.get("errorCode").asText());
    }

    @Test
    void 종목별_거래_타임라인을_조회하면_종목정보와_거래목록을_반환한다() throws Exception {
        FakeMarketDataPort marketDataPort = new FakeMarketDataPort();
        marketDataPort.add(SecurityInfoFixture.samsungElectronics(101L));
        FakeTradeLedgerPort tradeLedgerPort = new FakeTradeLedgerPort();
        tradeLedgerPort.add(101L, TradeTimelineInfoFixture.trade(501L, TradeSide.BUY, Instant.now()));
        MockMvc mockMvc = mockMvc(new FakeJournalRepository(), new FakeJournalTradeNoteRepository(), tradeLedgerPort, marketDataPort);

        MvcResult result = mockMvc.perform(get("/journal/trades").param("securityId", "101"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = readJson(result);
        assertEquals("005930", json.get("security").get("securityCode").asText());
        assertEquals(1, json.get("trades").size());
        assertEquals(501, json.get("trades").get(0).get("tradeId").asLong());
    }

    private MockMvc mockMvc(FakeJournalRepository journalRepository, FakeJournalTradeNoteRepository journalTradeNoteRepository,
                             FakeTradeLedgerPort tradeLedgerPort, FakeMarketDataPort marketDataPort) {
        return MockMvcBuilders.standaloneSetup(new JournalController(
                        journalService(journalRepository, journalTradeNoteRepository, tradeLedgerPort, marketDataPort)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private JournalService journalService(FakeJournalRepository journalRepository, FakeJournalTradeNoteRepository journalTradeNoteRepository,
                                           FakeTradeLedgerPort tradeLedgerPort, FakeMarketDataPort marketDataPort) {
        return new JournalService(journalRepository, journalTradeNoteRepository, tradeLedgerPort, marketDataPort);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Instant utc(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).toInstant(ZoneOffset.UTC);
    }

    private static Instant inFuture(long offsetSeconds) {
        return Instant.now().plusSeconds(offsetSeconds);
    }
}
