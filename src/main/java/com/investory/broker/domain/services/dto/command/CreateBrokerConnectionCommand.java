package com.investory.broker.domain.services.dto.command;

public record CreateBrokerConnectionCommand(
    Long userId,
    Long brokerId,
    String loginId,
    String password
) {
}
