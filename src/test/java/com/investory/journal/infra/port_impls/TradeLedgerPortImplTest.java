package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;
import com.investory.journal.domain.ports.dto.TradeTimelineInfo;
import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.ports.FakeAccountPort;
import com.investory.ledger.domain.ports.FakeMarketDataPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.repositories.FakeTradeRepository;
import com.investory.ledger.domain.services.TradeQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeLedgerPortImplTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long SECURITY_ID = 100L;

    private FakeTradeRepository tradeRepository;
    private TradeLedgerPortImpl port;

    @BeforeEach
    void setUp() {
        tradeRepository = new FakeTradeRepository();
        FakeAccountPort accountPort = new FakeAccountPort();
        accountPort.add(USER_ID, new AccountInfo(ACCOUNT_ID, "계좌", "123-**-****", "증권사"));
        FakeMarketDataPort marketDataPort = new FakeMarketDataPort();

        TradeQueryService tradeQueryService = new TradeQueryService(tradeRepository, accountPort, marketDataPort);
        port = new TradeLedgerPortImpl(tradeQueryService);
    }

    private Trade trade(LocalDate date, TradeSide side, int quantity) {
        return Trade.create(ACCOUNT_ID, SECURITY_ID, side, BigDecimal.valueOf(quantity), BigDecimal.TEN,
                BigDecimal.ONE, "ext-" + date + "-" + side + "-" + quantity,
                date.atStartOfDay(ZoneOffset.UTC).plusHours(1).toInstant());
    }

    @Test
    void 날짜범위_내_거래건수를_날짜별로_집계한다() {
        tradeRepository.add(
                trade(LocalDate.of(2026, 1, 1), TradeSide.BUY, 1),
                trade(LocalDate.of(2026, 1, 1), TradeSide.SELL, 2),
                trade(LocalDate.of(2026, 1, 2), TradeSide.BUY, 3),
                trade(LocalDate.of(2026, 1, 5), TradeSide.BUY, 4)
        );

        List<TradeCountInfo> result = port.countTradesByDateRange(USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.tradeDate().equals(LocalDate.of(2026, 1, 1)) && c.tradeCount() == 2));
        assertTrue(result.stream().anyMatch(c -> c.tradeDate().equals(LocalDate.of(2026, 1, 2)) && c.tradeCount() == 1));
    }

    @Test
    void 특정_날짜의_거래만_UTC_기준으로_조회한다() {
        tradeRepository.add(
                trade(LocalDate.of(2026, 1, 1), TradeSide.BUY, 1),
                trade(LocalDate.of(2026, 1, 2), TradeSide.SELL, 2)
        );

        List<TradeInfo> result = port.findTradesOn(USER_ID, LocalDate.of(2026, 1, 1));

        assertEquals(1, result.size());
        assertEquals(com.investory.journal.domain.constant.TradeSide.BUY, result.get(0).tradeSide());
        assertEquals(1, result.get(0).quantity());
    }

    @Test
    void 하루_거래가_페이지_크기를_넘어도_전부_모은다() {
        Trade[] trades = new Trade[150];
        for (int i = 0; i < 150; i++) {
            trades[i] = Trade.create(ACCOUNT_ID, SECURITY_ID, TradeSide.BUY,
                    BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, "ext-" + i,
                    LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).plusMinutes(i).toInstant());
        }
        tradeRepository.add(trades);

        List<TradeInfo> result = port.findTradesOn(USER_ID, LocalDate.of(2026, 1, 1));

        assertEquals(150, result.size());
    }

    @Test
    void 종목별_거래_타임라인을_기간과_페이지로_조회한다() {
        tradeRepository.add(
                trade(LocalDate.of(2026, 1, 1), TradeSide.BUY, 1),
                trade(LocalDate.of(2026, 1, 2), TradeSide.SELL, 2),
                trade(LocalDate.of(2026, 1, 3), TradeSide.BUY, 3)
        );

        List<TradeTimelineInfo> firstPage = port.findTradesBySecurity(
                USER_ID, SECURITY_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), 0, 2);

        assertEquals(2, firstPage.size());
    }

    @Test
    void 종목별_전체_건수는_페이지_크기와_무관하다() {
        tradeRepository.add(
                trade(LocalDate.of(2026, 1, 1), TradeSide.BUY, 1),
                trade(LocalDate.of(2026, 1, 2), TradeSide.SELL, 2),
                trade(LocalDate.of(2026, 1, 3), TradeSide.BUY, 3)
        );

        long count = port.countTradesBySecurity(USER_ID, SECURITY_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));

        assertEquals(3, count);
    }
}
