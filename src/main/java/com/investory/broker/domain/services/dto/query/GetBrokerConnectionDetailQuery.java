package com.investory.broker.domain.services.dto.query;

public record GetBrokerConnectionDetailQuery(
    Long userId,
    Long connectionId
) {
}
