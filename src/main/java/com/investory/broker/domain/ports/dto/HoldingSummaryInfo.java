package com.investory.broker.domain.ports.dto;

import java.math.BigDecimal;

public record HoldingSummaryInfo(
    int holdingCount,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedPnl
) {
    public static HoldingSummaryInfo empty() {
        return new HoldingSummaryInfo(0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
