package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.services.dto.result.HoldingSummaryResult;

import java.math.BigDecimal;

public record HoldingSummaryResponse(
    int holdingCount,
    BigDecimal totalPurchaseAmount,
    BigDecimal totalMarketValue,
    BigDecimal totalProfitLossAmount,
    BigDecimal totalReturnRate
) {
    public static HoldingSummaryResponse from(HoldingSummaryResult result) {
        return new HoldingSummaryResponse(result.holdingCount(), result.totalPurchaseAmount(),
                result.totalMarketValue(), result.totalProfitLossAmount(), result.totalReturnRate());
    }
}
