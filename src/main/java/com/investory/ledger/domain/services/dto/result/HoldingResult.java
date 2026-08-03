package com.investory.ledger.domain.services.dto.result;

import java.math.BigDecimal;

public record HoldingResult(
    Long securityId,
    String securityCode,
    String securityName,
    String marketType,
    String sectorName,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice,
    BigDecimal currentPrice,
    BigDecimal purchaseAmount,
    BigDecimal marketValue,
    BigDecimal profitLossAmount,
    BigDecimal returnRate,
    BigDecimal portfolioWeight
) {
}
