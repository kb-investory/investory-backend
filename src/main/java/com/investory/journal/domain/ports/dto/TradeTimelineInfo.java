package com.investory.journal.domain.ports.dto;

import com.investory.journal.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeTimelineInfo(
    Long tradeId,
    Long accountId,
    String accountName,
    TradeSide tradeSide,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal transactionCostAmount,
    Instant tradedAt
) {
}
