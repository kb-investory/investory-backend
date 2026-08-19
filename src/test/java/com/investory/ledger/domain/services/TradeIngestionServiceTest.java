package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.events.TradesIngestedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
    private CapturingEventPublisher eventPublisher;
    private TradeIngestionService tradeIngestionService;

    @BeforeEach
    void setUp() {
        tradeRepository = new FakeTradeRepository();
        marketDataPort = new FakeMarketDataPort();
        tradeMatchRepository = new FakeTradeMatchRepository();
        eventPublisher = new CapturingEventPublisher();
        TradeMatchingService tradeMatchingService = new TradeMatchingService(tradeRepository, tradeMatchRepository);
        tradeIngestionService = new TradeIngestionService(tradeRepository, marketDataPort, tradeMatchingService, eventPublisher);

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
        assertEquals(1, eventPublisher.events().size());
        TradesIngestedEvent event = eventPublisher.events().get(0);
        assertEquals(USER_ID, event.userId());
        assertEquals(ACCOUNT_ID, event.accountId());
        assertEquals(1, event.insertedTradeCount());
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
        // 새로 적재된 거래가 없는 두 번째 호출에서는 이벤트가 발행되지 않는다
        assertEquals(1, eventPublisher.events().size());
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

    @Test
    void 한_배치에_신규_중복_알수없는종목이_섞여있어도_각각_올바르게_처리한다() {
        Long otherSecurityId = 102L;
        marketDataPort.add(new SecurityInfo(otherSecurityId, "000660", "SK하이닉스", "KOSPI", "반도체"));
        RawTradeRecord existing = new RawTradeRecord("T-1", "005930", TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(500), Instant.parse("2026-07-29T01:15:00Z"));
        tradeIngestionService.ingestTrades(new IngestRawTradesCommand(USER_ID, ACCOUNT_ID, List.of(existing)));

        RawTradeRecord duplicate = existing;
        RawTradeRecord newTradeSameSecurity = new RawTradeRecord("T-2", "005930", TradeSide.SELL,
                BigDecimal.ONE, BigDecimal.valueOf(71000), BigDecimal.valueOf(100), Instant.parse("2026-07-30T01:15:00Z"));
        RawTradeRecord newTradeOtherSecurity = new RawTradeRecord("T-3", "000660", TradeSide.BUY,
                BigDecimal.valueOf(5), BigDecimal.valueOf(120000), BigDecimal.valueOf(300), Instant.parse("2026-07-30T02:15:00Z"));
        RawTradeRecord unknownSecurity = new RawTradeRecord("T-4", "999999", TradeSide.BUY,
                BigDecimal.ONE, BigDecimal.valueOf(1000), BigDecimal.ZERO, Instant.parse("2026-07-30T03:15:00Z"));

        IngestResult result = tradeIngestionService.ingestTrades(new IngestRawTradesCommand(
                USER_ID, ACCOUNT_ID, List.of(duplicate, newTradeSameSecurity, newTradeOtherSecurity, unknownSecurity)));

        assertEquals(2, result.successCount());
        assertEquals(1, result.skippedCount());
        assertTrue(result.skippedReasons().get(0).contains("999999"));
        List<Trade> saved = tradeRepository.search(new TradeSearchCriteria(List.of(ACCOUNT_ID), null, null, null, null, 0, 10));
        assertEquals(3, saved.size()); // 최초 1건 + 이번에 새로 적재된 2건
        // 첫 호출(setUp의 existing 적재)에서 1번, 이번 호출에서 새로 건드린 종목 2개(005930, 000660)로 2번 —
        // deleteCallCount는 두 호출 누적값이라 총 3
        assertEquals(3, tradeMatchRepository.deleteCallCount());
    }

    @Test
    void 알수없는_종목코드만_있으면_이벤트가_발행되지_않는다() {
        RawTradeRecord raw = new RawTradeRecord("T-1", "999999", TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(500), Instant.parse("2026-07-29T01:15:00Z"));

        tradeIngestionService.ingestTrades(new IngestRawTradesCommand(USER_ID, ACCOUNT_ID, List.of(raw)));

        assertTrue(eventPublisher.events().isEmpty());
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final List<TradesIngestedEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof TradesIngestedEvent tradesEvent) {
                events.add(tradesEvent);
            }
        }

        List<TradesIngestedEvent> events() {
            return events;
        }
    }
}
