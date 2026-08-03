package com.investory.broker.domain.ports.dto;

import java.math.BigDecimal;

public record RawHoldingRecord(
    String securityCode,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice,
    BigDecimal currentPrice
) {
}
