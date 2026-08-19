package com.investory.broker.domain.services.dto.command;

public record DisconnectBrokerConnectionCommand(
    Long userId,
    Long connectionId
) {
}
