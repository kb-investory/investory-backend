package com.investory.broker.domain.services.dto.query;

public record GetAccountDetailQuery(
    Long userId,
    Long accountId
) {
}
