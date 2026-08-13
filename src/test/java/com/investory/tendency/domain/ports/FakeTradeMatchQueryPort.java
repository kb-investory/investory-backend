package com.investory.tendency.domain.ports;

import java.util.List;

public class FakeTradeMatchQueryPort implements TradeMatchQueryPort {

    private List<Integer> holdingDays = List.of();

    public void setHoldingDays(List<Integer> holdingDays) {
        this.holdingDays = holdingDays;
    }

    @Override
    public List<Integer> findHoldingDaysForLast90Days(Long userId) {
        return holdingDays;
    }
}
