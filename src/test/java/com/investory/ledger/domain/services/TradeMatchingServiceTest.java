package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.model.TradeMatch;
import com.investory.ledger.domain.repositories.FakeTradeMatchRepository;
import com.investory.ledger.domain.repositories.FakeTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeMatchingServiceTest {

    private static final Long ACCOUNT_ID = 11L;
    private static final Long SECURITY_ID = 101L;

    private FakeTradeRepository tradeRepository;
    private FakeTradeMatchRepository tradeMatchRepository;
    private TradeMatchingService tradeMatchingService;

    @BeforeEach
    void setUp() {
        tradeRepository = new FakeTradeRepository();
        tradeMatchRepository = new FakeTradeMatchRepository();
        tradeMatchingService = new TradeMatchingService(tradeRepository, tradeMatchRepository);
    }

    @Test
    void 단일_매수와_매도가_전량_매칭된다() {
        tradeRepository.add(
                buy(BigDecimal.TEN, BigDecimal.valueOf(1000), BigDecimal.ZERO, "2026-07-01T00:00:00Z"),
                sell(BigDecimal.TEN, BigDecimal.valueOf(1200), BigDecimal.ZERO, "2026-07-11T00:00:00Z")
        );

        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);

        List<TradeMatch> matches = tradeMatchRepository.all();
        assertEquals(1, matches.size());
        TradeMatch match = matches.get(0);
        assertEquals(0, match.getMatchedQuantity().compareTo(BigDecimal.TEN));
        assertEquals(0, match.getRealizedPnl().compareTo(BigDecimal.valueOf(2000))); // (1200-1000)*10
        assertEquals(0, match.getReturnRate().compareTo(BigDecimal.valueOf(20))); // 2000/10000*100
        assertEquals(10, match.getHoldingDays());
    }

    @Test
    void 거래비용이_있으면_실현손익에서_차감된다() {
        tradeRepository.add(
                buy(BigDecimal.TEN, BigDecimal.valueOf(1000), BigDecimal.valueOf(100), "2026-07-01T00:00:00Z"),
                sell(BigDecimal.TEN, BigDecimal.valueOf(1200), BigDecimal.valueOf(200), "2026-07-11T00:00:00Z")
        );

        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);

        TradeMatch match = tradeMatchRepository.all().get(0);
        // gross=2000, cost=(10+20)*10=300 => realized=1700
        assertEquals(0, match.getRealizedPnl().compareTo(BigDecimal.valueOf(1700)));
    }

    @Test
    void 하나의_매도가_여러_매수와_분할_매칭된다() {
        tradeRepository.add(
                buy(BigDecimal.valueOf(5), BigDecimal.valueOf(1000), BigDecimal.ZERO, "2026-07-01T00:00:00Z"),
                buy(BigDecimal.valueOf(5), BigDecimal.valueOf(1100), BigDecimal.ZERO, "2026-07-05T00:00:00Z"),
                sell(BigDecimal.TEN, BigDecimal.valueOf(1300), BigDecimal.ZERO, "2026-07-20T00:00:00Z")
        );

        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);

        List<TradeMatch> matches = tradeMatchRepository.all();
        assertEquals(2, matches.size());

        TradeMatch first = matches.get(0);
        assertEquals(0, first.getBuyPrice().compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, first.getMatchedQuantity().compareTo(BigDecimal.valueOf(5)));
        assertEquals(0, first.getRealizedPnl().compareTo(BigDecimal.valueOf(1500))); // (1300-1000)*5
        assertEquals(19, first.getHoldingDays());

        TradeMatch second = matches.get(1);
        assertEquals(0, second.getBuyPrice().compareTo(BigDecimal.valueOf(1100)));
        assertEquals(0, second.getMatchedQuantity().compareTo(BigDecimal.valueOf(5)));
        assertEquals(0, second.getRealizedPnl().compareTo(BigDecimal.valueOf(1000))); // (1300-1100)*5
        assertEquals(15, second.getHoldingDays());
    }

    @Test
    void 매도수량이_보유수량을_초과하면_남는_수량은_매칭없이_버려진다() {
        tradeRepository.add(
                buy(BigDecimal.valueOf(5), BigDecimal.valueOf(1000), BigDecimal.ZERO, "2026-07-01T00:00:00Z"),
                sell(BigDecimal.valueOf(8), BigDecimal.valueOf(1200), BigDecimal.ZERO, "2026-07-11T00:00:00Z")
        );

        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);

        List<TradeMatch> matches = tradeMatchRepository.all();
        assertEquals(1, matches.size());
        assertEquals(0, matches.get(0).getMatchedQuantity().compareTo(BigDecimal.valueOf(5)));
    }

    @Test
    void 재계산하면_기존_매칭을_지우고_다시_만든다() {
        tradeRepository.add(
                buy(BigDecimal.TEN, BigDecimal.valueOf(1000), BigDecimal.ZERO, "2026-07-01T00:00:00Z"),
                sell(BigDecimal.TEN, BigDecimal.valueOf(1200), BigDecimal.ZERO, "2026-07-11T00:00:00Z")
        );

        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);
        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);

        assertEquals(2, tradeMatchRepository.deleteCallCount());
        assertEquals(1, tradeMatchRepository.all().size());
    }

    @Test
    void 매수만_있고_매도가_없으면_매칭이_생기지_않는다() {
        tradeRepository.add(buy(BigDecimal.TEN, BigDecimal.valueOf(1000), BigDecimal.ZERO, "2026-07-01T00:00:00Z"));

        tradeMatchingService.rematch(ACCOUNT_ID, SECURITY_ID);

        assertTrue(tradeMatchRepository.all().isEmpty());
    }

    private Trade buy(BigDecimal quantity, BigDecimal unitPrice, BigDecimal cost, String tradedAt) {
        return Trade.create(ACCOUNT_ID, SECURITY_ID, TradeSide.BUY, quantity, unitPrice, cost,
                "ext-" + tradedAt, Instant.parse(tradedAt));
    }

    private Trade sell(BigDecimal quantity, BigDecimal unitPrice, BigDecimal cost, String tradedAt) {
        return Trade.create(ACCOUNT_ID, SECURITY_ID, TradeSide.SELL, quantity, unitPrice, cost,
                "ext-" + tradedAt, Instant.parse(tradedAt));
    }
}
