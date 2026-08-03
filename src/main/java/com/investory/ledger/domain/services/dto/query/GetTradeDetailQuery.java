package com.investory.ledger.domain.services.dto.query;

public record GetTradeDetailQuery(
    Long userId,
    Long tradeId
) {
}
