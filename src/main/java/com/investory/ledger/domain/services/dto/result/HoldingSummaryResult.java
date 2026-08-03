package com.investory.ledger.domain.services.dto.result;

import java.math.BigDecimal;

public record HoldingSummaryResult(
    int holdingCount,
    BigDecimal totalPurchaseAmount,
    BigDecimal totalMarketValue,
    BigDecimal totalProfitLossAmount,
    BigDecimal totalReturnRate
) {
}
