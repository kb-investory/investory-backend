package com.investory.broker.domain.services.dto.query;

public record GetConnectionAccountsQuery(
    Long userId,
    Long connectionId
) {
}
