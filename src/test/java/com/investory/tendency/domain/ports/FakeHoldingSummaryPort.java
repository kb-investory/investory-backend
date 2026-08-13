package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.HoldingWeightInfo;

import java.util.List;

public class FakeHoldingSummaryPort implements HoldingSummaryPort {

    private List<HoldingWeightInfo> weights = List.of();

    public void setWeights(List<HoldingWeightInfo> weights) {
        this.weights = weights;
    }

    @Override
    public List<HoldingWeightInfo> findHoldingWeights(Long userId) {
        return weights;
    }
}
