package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.AccountHoldingsInfo;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;

import java.util.List;

public class FakeHoldingDetailPort implements HoldingDetailPort {

    private AccountHoldingsInfo nextResult = new AccountHoldingsInfo(HoldingSummaryInfo.empty(), List.of());

    public void willReturn(AccountHoldingsInfo result) {
        this.nextResult = result;
    }

    @Override
    public AccountHoldingsInfo getHoldings(Long userId, Long accountId) {
        return nextResult;
    }
}
