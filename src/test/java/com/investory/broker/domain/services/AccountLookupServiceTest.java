package com.investory.broker.domain.services;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.dto.result.InvestmentAccountResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountLookupServiceTest {

    private FakeBrokerConnectionRepository brokerConnectionRepository;
    private FakeInvestmentAccountRepository investmentAccountRepository;
    private AccountLookupService accountLookupService;

    @BeforeEach
    void setUp() {
        brokerConnectionRepository = new FakeBrokerConnectionRepository();
        investmentAccountRepository = new FakeInvestmentAccountRepository();
        accountLookupService = new AccountLookupService(investmentAccountRepository, brokerConnectionRepository);
    }

    @Test
    void 사용자ID로_전체_계좌를_조회하면_브로커명이_채워진다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌", AccountType.STOCK, "KRW"));

        List<InvestmentAccountResult> results = accountLookupService.findByUserId(1L);

        assertEquals(1, results.size());
        assertEquals("미래에셋증권(모의)", results.get(0).brokerName());
    }

    @Test
    void accountId와_userId로_단건_조회한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌", AccountType.STOCK, "KRW"));

        Optional<InvestmentAccountResult> result = accountLookupService.findByIdAndUserId(25L, 1L);

        assertTrue(result.isPresent());
        assertEquals("종합주식계좌", result.get().accountName());
    }

    @Test
    void accountId_목록으로_일괄_조회한다() {
        brokerConnectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        investmentAccountRepository.add(1L, InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌", AccountType.STOCK, "KRW"));

        List<InvestmentAccountResult> results = accountLookupService.findByIds(List.of(25L));

        assertEquals(1, results.size());
        assertEquals(25L, results.get(0).accountId());
    }
}
