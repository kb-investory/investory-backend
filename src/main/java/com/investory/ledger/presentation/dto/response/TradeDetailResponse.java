package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.services.dto.result.TradeDetailResult;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeDetailResponse(
    Long tradeId,
    AccountResponse account,
    SecurityResponse security,
    TradeSide tradeSide,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal tradeAmount,
    BigDecimal transactionCostAmount,
    BigDecimal settlementAmount,
    Instant tradedAt
) {
    public static TradeDetailResponse from(TradeDetailResult result) {
        return new TradeDetailResponse(
                result.tradeId(),
                AccountResponse.from(result.account()),
                SecurityResponse.from(result.security()),
                result.tradeSide(),
                result.quantity(),
                result.unitPrice(),
                result.tradeAmount(),
                result.transactionCostAmount(),
                result.settlementAmount(),
                result.tradedAt()
        );
    }
}
