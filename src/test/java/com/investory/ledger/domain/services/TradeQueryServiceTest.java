package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.exception.LedgerErrorCode;
import com.investory.ledger.domain.exception.LedgerException;
import com.investory.ledger.domain.model.TradeFixture;
import com.investory.ledger.domain.ports.FakeAccountPort;
import com.investory.ledger.domain.ports.FakeMarketDataPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.FakeTradeRepository;
import com.investory.ledger.domain.repositories.TradeSearchCriteria;
import com.investory.ledger.domain.services.dto.query.GetTradeDetailQuery;
import com.investory.ledger.domain.services.dto.query.GetTradesQuery;
import com.investory.ledger.domain.services.dto.result.TradeDetailResult;
import com.investory.ledger.domain.services.dto.result.TradeListResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeQueryServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long ACCOUNT_ID = 11L;
    private static final Long SECURITY_ID = 101L;

    private FakeTradeRepository tradeRepository;
    private FakeAccountPort accountPort;
    private FakeMarketDataPort marketDataPort;
    private TradeQueryService tradeQueryService;

    @BeforeEach
    void setUp() {
        tradeRepository = new FakeTradeRepository();
        accountPort = new FakeAccountPort();
        marketDataPort = new FakeMarketDataPort();
        tradeQueryService = new TradeQueryService(tradeRepository, accountPort, marketDataPort);

        accountPort.add(USER_ID, new AccountInfo(ACCOUNT_ID, "국내주식계좌", "1234-****-5678", "한국투자증권"));
        marketDataPort.add(new SecurityInfo(SECURITY_ID, "005930", "삼성전자", "KOSPI", "반도체"));
    }

    @Test
    void 계좌_필터_없이_조회하면_전체_계좌_거래를_최신순으로_반환한다() {
        tradeRepository.add(
                TradeFixture.trade(ACCOUNT_ID, SECURITY_ID, TradeSide.BUY, "T-1", Instant.parse("2026-07-10T01:00:00Z")),
                TradeFixture.trade(ACCOUNT_ID, SECURITY_ID, TradeSide.SELL, "T-2", Instant.parse("2026-07-20T01:00:00Z"))
        );

        TradeListResult result = tradeQueryService.getTrades(
                new GetTradesQuery(USER_ID, null, null, null, null, null, 0, 20));

        assertEquals(2, result.content().size());
        assertEquals(TradeSide.SELL, result.content().get(0).tradeSide());
        assertEquals(2, result.totalElements());
        assertEquals(false, result.hasNext());
        assertEquals("삼성전자", result.content().get(0).securityName());
        assertEquals("국내주식계좌", result.content().get(0).accountName());
    }

    @Test
    void 조회_시작일이_종료일보다_늦으면_예외() {
        LedgerException exception = assertThrows(LedgerException.class, () -> tradeQueryService.getTrades(
                new GetTradesQuery(USER_ID, null, null, null,
                        LocalDate.of(2026, 7, 30), LocalDate.of(2026, 7, 1), 0, 20)));

        assertEquals(LedgerErrorCode.LEDGER_INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    void 잘못된_매매구분이면_예외() {
        LedgerException exception = assertThrows(LedgerException.class, () -> tradeQueryService.getTrades(
                new GetTradesQuery(USER_ID, null, null, "HOLD", null, null, 0, 20)));

        assertEquals(LedgerErrorCode.LEDGER_INVALID_TRADE_SIDE, exception.getErrorCode());
    }

    @Test
    void 조회_개수가_최대치를_넘으면_예외() {
        LedgerException exception = assertThrows(LedgerException.class, () -> tradeQueryService.getTrades(
                new GetTradesQuery(USER_ID, null, null, null, null, null, 0, 200)));

        assertEquals(LedgerErrorCode.LEDGER_INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void 소유하지_않은_계좌를_조회하면_예외() {
        LedgerException exception = assertThrows(LedgerException.class, () -> tradeQueryService.getTrades(
                new GetTradesQuery(USER_ID, 999L, null, null, null, null, 0, 20)));

        assertEquals(LedgerErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 존재하지_않는_거래를_상세조회하면_거래없음_예외() {
        LedgerException exception = assertThrows(LedgerException.class, () -> tradeQueryService.getTradeDetail(
                new GetTradeDetailQuery(USER_ID, 999L)));

        assertEquals(LedgerErrorCode.TRADE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 다른_사용자_소유_거래를_상세조회하면_거래없음_예외로_구분없이_처리한다() {
        Long otherUsersAccountId = 22L; // FakeAccountPort에 USER_ID 소유로 등록하지 않음
        tradeRepository.add(TradeFixture.trade(otherUsersAccountId, SECURITY_ID, TradeSide.BUY, "T-1", Instant.parse("2026-07-10T01:00:00Z")));
        Long tradeId = tradeRepository.search(new TradeSearchCriteria(
                List.of(otherUsersAccountId), null, null, null, null, 0, 10)).get(0).getTradeId();

        LedgerException exception = assertThrows(LedgerException.class, () -> tradeQueryService.getTradeDetail(
                new GetTradeDetailQuery(USER_ID, tradeId)));

        assertEquals(LedgerErrorCode.TRADE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 거래_상세조회시_settlementAmount는_매수는_더하고_매도는_뺀다() {
        tradeRepository.add(TradeFixture.trade(ACCOUNT_ID, SECURITY_ID, TradeSide.BUY, "T-1", Instant.parse("2026-07-10T01:00:00Z")));
        Long tradeId = tradeRepository.search(new TradeSearchCriteria(
                List.of(ACCOUNT_ID), null, null, null, null, 0, 10)).get(0).getTradeId();

        TradeDetailResult result = tradeQueryService.getTradeDetail(new GetTradeDetailQuery(USER_ID, tradeId));

        // quantity=10, unitPrice=10000 => tradeAmount=100000, cost=100 => settlement = 100100 (매수)
        assertEquals(0, result.settlementAmount().compareTo(BigDecimal.valueOf(100100)));
        assertEquals("한국투자증권", result.account().brokerageName());
        assertEquals("KOSPI", result.security().marketType());
    }
}
