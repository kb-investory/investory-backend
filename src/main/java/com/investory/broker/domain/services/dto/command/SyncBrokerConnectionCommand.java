package com.investory.broker.domain.services.dto.command;

public record SyncBrokerConnectionCommand(
    Long userId,
    Long connectionId
) {
}
