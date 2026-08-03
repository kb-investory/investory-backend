package com.investory.ledger.domain.services.dto.result;

import com.investory.ledger.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResult(
    Long tradeId,
    Long accountId,
    String accountName,
    Long securityId,
    String securityCode,
    String securityName,
    String marketType,
    TradeSide tradeSide,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal tradeAmount,
    BigDecimal transactionCostAmount,
    Instant tradedAt
) {
}
