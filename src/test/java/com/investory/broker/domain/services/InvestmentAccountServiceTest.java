package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.exception.BrokerErrorCode;
import com.investory.broker.domain.exception.BrokerException;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.ports.FakeHoldingSummaryPort;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.dto.query.GetConnectionAccountsQuery;
import com.investory.broker.domain.services.dto.result.AccountListResult;
import com.investory.broker.domain.services.dto.result.ConnectionAccountsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestmentAccountServiceTest {

    private FakeBrokerConnectionRepository brokerConnectionRepository;
    private FakeInvestmentAccountRepository investmentAccountRepository;
    private FakeHoldingSummaryPort holdingSummaryPort;
    private InvestmentAccountService investmentAccountService;

    @BeforeEach
    void setUp() {
        brokerConnectionRepository = new FakeBrokerConnectionRepository();
        investmentAccountRepository = new FakeInvestmentAccountRepository();
        holdingSummaryPort = new FakeHoldingSummaryPort();
        investmentAccountService = new InvestmentAccountService(
                investmentAccountRepository, brokerConnectionRepository, holdingSummaryPort);
    }

    @Test
    void 연결의_계좌_목록과_보유현황_요약을_반환한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌", AccountType.STOCK, "KRW"));
        holdingSummaryPort.willReturn(new HoldingSummaryInfo(3, BigDecimal.valueOf(8420000), BigDecimal.valueOf(320000)));

        ConnectionAccountsResult result = investmentAccountService.getAccountsByConnection(
                new GetConnectionAccountsQuery(1L, 15L));

        assertEquals(15L, result.connectionId());
        assertEquals("미래에셋증권(모의)", result.brokerName());
        assertEquals(1, result.accounts().size());
        ConnectionAccountsResult.AccountSummary account = result.accounts().get(0);
        assertEquals(25L, account.accountId());
        assertEquals(3, account.holdingCount());
        assertEquals(0, BigDecimal.valueOf(8420000).compareTo(account.totalMarketValue()));
    }

    @Test
    void 계좌가_없으면_빈_목록을_반환한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));

        ConnectionAccountsResult result = investmentAccountService.getAccountsByConnection(
                new GetConnectionAccountsQuery(1L, 15L));

        assertTrue(result.accounts().isEmpty());
    }

    @Test
    void 존재하지_않거나_본인_소유가_아닌_연결이면_예외가_발생한다() {
        brokerConnectionRepository.add(2L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        GetConnectionAccountsQuery query = new GetConnectionAccountsQuery(1L, 15L);

        BrokerException exception = assertThrows(BrokerException.class, () -> investmentAccountService.getAccountsByConnection(query));

        assertEquals(BrokerErrorCode.CONNECTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 전체_계좌_목록과_합산_요약을_반환한다() {
        Instant lastSyncedAt = Instant.parse("2026-07-29T15:00:03Z");
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connection(
                15L, 1L, "S9990001A", "미래에셋증권(모의)", ConnectionStatus.CONNECTED,
                Instant.parse("2026-07-29T13:40:00Z"), lastSyncedAt, 1));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌", AccountType.STOCK, "KRW"));
        holdingSummaryPort.willReturn(new HoldingSummaryInfo(3, BigDecimal.valueOf(8420000), BigDecimal.valueOf(320000)));

        AccountListResult result = investmentAccountService.getAccounts(1L);

        assertEquals(1, result.summary().accountCount());
        assertEquals(0, BigDecimal.valueOf(8420000).compareTo(result.summary().totalMarketValue()));
        assertEquals(0, BigDecimal.valueOf(320000).compareTo(result.summary().totalUnrealizedPnl()));
        AccountListResult.AccountResult account = result.accounts().get(0);
        assertEquals(25L, account.accountId());
        assertEquals(1L, account.brokerId());
        assertEquals("미래에셋증권(모의)", account.brokerName());
        assertEquals(lastSyncedAt, account.lastSyncedAt());
    }

    @Test
    void 계좌가_없으면_전체_목록에서도_빈_목록과_0_요약을_반환한다() {
        AccountListResult result = investmentAccountService.getAccounts(1L);

        assertEquals(0, result.summary().accountCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.summary().totalMarketValue()));
        assertTrue(result.accounts().isEmpty());
    }
}
