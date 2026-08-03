package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.services.dto.result.TradeResult;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
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
    public static TradeResponse from(TradeResult result) {
        return new TradeResponse(
                result.tradeId(),
                result.accountId(),
                result.accountName(),
                result.securityId(),
                result.securityCode(),
                result.securityName(),
                result.marketType(),
                result.tradeSide(),
                result.quantity(),
                result.unitPrice(),
                result.tradeAmount(),
                result.transactionCostAmount(),
                result.tradedAt()
        );
    }
}
