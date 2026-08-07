package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.ports.FakeMarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.FakeTradeMatchRepository;
import com.investory.ledger.domain.repositories.FakeTradeRepository;
import com.investory.ledger.domain.repositories.TradeSearchCriteria;
import com.investory.ledger.domain.services.dto.command.IngestRawTradesCommand;
import com.investory.ledger.domain.services.dto.command.RawTradeRecord;
import com.investory.ledger.domain.services.dto.result.IngestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeIngestionServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long ACCOUNT_ID = 11L;
    private static final Long SECURITY_ID = 101L;

    private FakeTradeRepository tradeRepository;
    private FakeMarketDataPort marketDataPort;
    private FakeTradeMatchRepository tradeMatchRepository;
    private TradeIngestionService tradeIngestionService;

    @BeforeEach
    void setUp() {
        tradeRepository = new FakeTradeRepository();
        marketDataPort = new FakeMarketDataPort();
        tradeMatchRepository = new FakeTradeMatchRepository();
        TradeMatchingService tradeMatchingService = new TradeMatchingService(tradeRepository, tradeMatchRepository);
        tradeIngestionService = new TradeIngestionService(tradeRepository, marketDataPort, tradeMatchingService);

        marketDataPort.add(new SecurityInfo(SECURITY_ID, "005930", "삼성전자", "KOSPI", "반도체"));
    }

    @Test
    void 원시_거래를_받아서_적재한다() {
        RawTradeRecord raw = new RawTradeRecord("T-1", "005930", TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(500), Instant.parse("2026-07-29T01:15:00Z"));

        IngestResult result = tradeIngestionService.ingestTrades(new IngestRawTradesCommand(USER_ID, ACCOUNT_ID, List.of(raw)));

        assertEquals(1, result.successCount());
        assertEquals(0, result.skippedCount());
        List<Trade> saved = tradeRepository.search(new TradeSearchCriteria(List.of(ACCOUNT_ID), null, null, null, null, 0, 10));
        assertEquals(1, saved.size());
        assertEquals(SECURITY_ID, saved.get(0).getSecurityId());
        assertEquals(1, tradeMatchRepository.deleteCallCount());
    }

    @Test
    void 이미_적재된_외부거래ID는_다시_적재하지_않는다() {
        RawTradeRecord raw = new RawTradeRecord("T-1", "005930", TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(500), Instant.parse("2026-07-29T01:15:00Z"));
        tradeIngestionService.ingestTrades(new IngestRawTradesCommand(USER_ID, ACCOUNT_ID, List.of(raw)));

        IngestResult result = tradeIngestionService.ingestTrades(new IngestRawTradesCommand(USER_ID, ACCOUNT_ID, List.of(raw)));

        assertEquals(0, result.successCount());
        assertEquals(0, result.skippedCount());
        List<Trade> saved = tradeRepository.search(new TradeSearchCriteria(List.of(ACCOUNT_ID), null, null, null, null, 0, 10));
        assertEquals(1, saved.size());
        // 두 번째 호출은 전부 중복이라 새로 건드린 종목이 없어 재매칭이 트리거되지 않는다
        assertEquals(1, tradeMatchRepository.deleteCallCount());
    }

    @Test
    void 알수_없는_종목코드는_건너뛰고_사유를_기록한다() {
        RawTradeRecord raw = new RawTradeRecord("T-1", "999999", TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(500), Instant.parse("2026-07-29T01:15:00Z"));

        IngestResult result = tradeIngestionService.ingestTrades(new IngestRawTradesCommand(USER_ID, ACCOUNT_ID, List.of(raw)));

        assertEquals(0, result.successCount());
        assertEquals(1, result.skippedCount());
        assertTrue(result.skippedReasons().get(0).contains("999999"));
        assertTrue(tradeRepository.search(new TradeSearchCriteria(List.of(ACCOUNT_ID), null, null, null, null, 0, 10)).isEmpty());
        assertEquals(0, tradeMatchRepository.deleteCallCount());
    }
}
