package com.investory.journal.domain.ports.dto;

import com.investory.journal.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeInfo(
    Long tradeId,
    Long securityId,
    TradeSide tradeSide,
    int quantity,
    BigDecimal unitPrice,
    Instant tradedAt
) {
}
