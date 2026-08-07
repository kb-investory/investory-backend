package com.investory.ledger.domain.services.dto.query;

public record GetHoldingsQuery(
    Long userId,
    Long accountId,
    Long securityId
) {
}
