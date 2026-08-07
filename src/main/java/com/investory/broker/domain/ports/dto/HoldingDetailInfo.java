package com.investory.broker.domain.ports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HoldingDetailInfo(
    Long securityId,
    String securityCode,
    String securityName,
    String marketType,
    BigDecimal quantity,
    BigDecimal averageCost,
    BigDecimal marketValue,
    BigDecimal unrealizedPnl,
    BigDecimal portfolioWeight,
    LocalDate snapshotDate
) {
}
