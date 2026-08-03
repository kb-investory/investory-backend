package com.investory.broker.domain.ports.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RawTradeRecord(
    String externalTradeId,
    String securityCode,
    String tradeSide,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal transactionCostAmount,
    Instant tradedAt
) {
}
