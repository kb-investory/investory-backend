package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.services.dto.result.HoldingResult;

import java.math.BigDecimal;

public record HoldingResponse(
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
    public static HoldingResponse from(HoldingResult result) {
        return new HoldingResponse(
                result.securityId(),
                result.securityCode(),
                result.securityName(),
                result.marketType(),
                result.sectorName(),
                result.quantity(),
                result.averagePurchasePrice(),
                result.currentPrice(),
                result.purchaseAmount(),
                result.marketValue(),
                result.profitLossAmount(),
                result.returnRate(),
                result.portfolioWeight()
        );
    }
}
