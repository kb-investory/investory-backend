package com.investory.ledger.domain.services.dto.command;

import com.investory.ledger.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

// broker가 Mock Broker(또는 향후 실 증권사) 원시 응답을 매핑해서 넘기는 값.
public record RawTradeRecord(
    String externalTradeId,
    String securityCode,
    TradeSide tradeSide,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal transactionCostAmount,
    Instant tradedAt
) {
}
