package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;

public class FakeHoldingSummaryPort implements HoldingSummaryPort {

    private HoldingSummaryInfo nextResult = HoldingSummaryInfo.empty();

    public void willReturn(HoldingSummaryInfo result) {
        this.nextResult = result;
    }

    @Override
    public HoldingSummaryInfo summarize(Long userId, Long accountId) {
        return nextResult;
    }
}
