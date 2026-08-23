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
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        tradeIngestionService = new TradeIngestionService(
                tradeRepository, marketDataPort, tradeMatchingService, eventPublisher, new FakeTransactionManager());

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

    // 실제 doIngestTrades()/repository 페이크로 데드락 재시도를 재현하려면 "재시도 시 이전 시도의
    // insert가 롤백돼 있어야" 정확한데, 인메모리 페이크는 진짜 트랜잭션 롤백을 흉내내지 않는다(예:
    // FakeTradeRepository는 실패한 시도에서 넣은 거래를 되돌리지 않아, 재시도 시 중복으로 오인해
    // 건너뛴다). 그래서 재시도 메커니즘 자체는 business 로직과 분리해 retryOnDeadlock()을 직접
    // 검증한다(#203).
    @Test
    void 데드락이_MAX_ATTEMPTS_미만이면_재시도_후_성공한다() {
        AtomicInteger callCount = new AtomicInteger();

        String result = tradeIngestionService.retryOnDeadlock(() -> {
            if (callCount.incrementAndGet() <= 2) {
                throw new DeadlockLoserDataAccessException("simulated deadlock", null);
            }
            return "ok";
        }, ACCOUNT_ID);

        assertEquals("ok", result);
        assertEquals(3, callCount.get()); // 실패 2번 + 성공 1번
    }

    @Test
    void 데드락이_MAX_ATTEMPTS_이상_반복되면_예외를_그대로_전파한다() {
        AtomicInteger callCount = new AtomicInteger();

        assertThrows(DeadlockLoserDataAccessException.class, () -> tradeIngestionService.retryOnDeadlock(() -> {
            callCount.incrementAndGet();
            throw new DeadlockLoserDataAccessException("simulated deadlock", null);
        }, ACCOUNT_ID));

        assertEquals(3, callCount.get()); // MAX_ATTEMPTS만큼만 시도하고 더는 재시도하지 않음
    }

    @Test
    void 데드락이_아닌_예외는_재시도하지_않고_바로_전파한다() {
        AtomicInteger callCount = new AtomicInteger();

        assertThrows(IllegalStateException.class, () -> tradeIngestionService.retryOnDeadlock(() -> {
            callCount.incrementAndGet();
            throw new IllegalStateException("데드락이 아닌 다른 오류");
        }, ACCOUNT_ID));

        assertEquals(1, callCount.get());
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
