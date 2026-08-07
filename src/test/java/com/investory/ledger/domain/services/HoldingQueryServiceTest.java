package com.investory.ledger.domain.services;

import com.investory.ledger.domain.model.HoldingFixture;
import com.investory.ledger.domain.ports.FakeAccountPort;
import com.investory.ledger.domain.ports.FakeMarketDataPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.FakeHoldingSnapshotRepository;
import com.investory.ledger.domain.services.dto.query.GetHoldingsQuery;
import com.investory.ledger.domain.services.dto.result.HoldingListResult;
import com.investory.ledger.domain.services.dto.result.HoldingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldingQueryServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long ACCOUNT_1 = 11L;
    private static final Long ACCOUNT_2 = 12L;
    private static final Long SECURITY_ID = 101L;
    private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 7, 29);

    private FakeHoldingSnapshotRepository holdingSnapshotRepository;
    private FakeAccountPort accountPort;
    private FakeMarketDataPort marketDataPort;
    private HoldingQueryService holdingQueryService;

    @BeforeEach
    void setUp() {
        holdingSnapshotRepository = new FakeHoldingSnapshotRepository();
        accountPort = new FakeAccountPort();
        marketDataPort = new FakeMarketDataPort();
        holdingQueryService = new HoldingQueryService(holdingSnapshotRepository, accountPort, marketDataPort);

        accountPort.add(USER_ID,
                new AccountInfo(ACCOUNT_1, "국내주식계좌1", "1111-****-1111", "한국투자증권"),
                new AccountInfo(ACCOUNT_2, "국내주식계좌2", "2222-****-2222", "한국투자증권"));
        marketDataPort.add(new SecurityInfo(SECURITY_ID, "005930", "삼성전자", "KOSPI", "반도체"));
    }

    @Test
    void 여러_계좌가_같은_종목을_보유하면_통합해서_반환한다() {
        holdingSnapshotRepository.add(
                HoldingFixture.holding(ACCOUNT_1, SECURITY_ID, BigDecimal.TEN, BigDecimal.valueOf(60000), BigDecimal.valueOf(90000), SNAPSHOT_DATE),
                HoldingFixture.holding(ACCOUNT_2, SECURITY_ID, BigDecimal.TEN, BigDecimal.valueOf(100000), BigDecimal.valueOf(90000), SNAPSHOT_DATE)
        );

        HoldingListResult result = holdingQueryService.getHoldings(new GetHoldingsQuery(USER_ID, null, null));

        assertEquals(1, result.holdings().size());
        HoldingResult holding = result.holdings().get(0);
        assertEquals(0, holding.quantity().compareTo(BigDecimal.valueOf(20)));
        assertEquals(0, holding.averagePurchasePrice().compareTo(BigDecimal.valueOf(80000)));
        assertEquals(0, holding.purchaseAmount().compareTo(BigDecimal.valueOf(1600000)));
        assertEquals(0, holding.marketValue().compareTo(BigDecimal.valueOf(1800000)));
        assertEquals(0, holding.profitLossAmount().compareTo(BigDecimal.valueOf(200000)));
        assertEquals(0, holding.returnRate().compareTo(BigDecimal.valueOf(12.5)));
        assertEquals(0, holding.portfolioWeight().compareTo(BigDecimal.valueOf(100)));

        assertEquals(SNAPSHOT_DATE, result.snapshotDate());
        assertEquals(0, result.summary().totalPurchaseAmount().compareTo(BigDecimal.valueOf(1600000)));
        assertEquals(0, result.summary().totalReturnRate().compareTo(BigDecimal.valueOf(12.5)));
    }

    @Test
    void 보유수량이_0인_종목은_제외한다() {
        Long soldOutSecurityId = 102L;
        marketDataPort.add(new SecurityInfo(soldOutSecurityId, "000660", "SK하이닉스", "KOSPI", "반도체"));
        holdingSnapshotRepository.add(
                HoldingFixture.holding(ACCOUNT_1, SECURITY_ID, BigDecimal.TEN, BigDecimal.valueOf(60000), BigDecimal.valueOf(90000), SNAPSHOT_DATE),
                HoldingFixture.holding(ACCOUNT_1, soldOutSecurityId, BigDecimal.ZERO, BigDecimal.valueOf(60000), BigDecimal.valueOf(90000), SNAPSHOT_DATE)
        );

        HoldingListResult result = holdingQueryService.getHoldings(new GetHoldingsQuery(USER_ID, null, null));

        assertEquals(1, result.holdings().size());
        assertEquals(SECURITY_ID, result.holdings().get(0).securityId());
    }

    @Test
    void 보유_종목이_없으면_summary는_전부_0이고_holdings는_빈_리스트다() {
        HoldingListResult result = holdingQueryService.getHoldings(new GetHoldingsQuery(USER_ID, null, null));

        assertTrue(result.holdings().isEmpty());
        assertEquals(null, result.snapshotDate());
        assertEquals(0, result.summary().holdingCount());
        assertEquals(0, result.summary().totalPurchaseAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.summary().totalReturnRate().compareTo(BigDecimal.ZERO));
    }
}
