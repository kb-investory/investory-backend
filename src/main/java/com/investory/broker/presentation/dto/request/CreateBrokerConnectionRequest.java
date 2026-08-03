package com.investory.broker.presentation.dto.request;

import com.investory.broker.domain.services.dto.command.CreateBrokerConnectionCommand;

public record CreateBrokerConnectionRequest(
    Long brokerId,
    String loginId,
    String password
) {
    public CreateBrokerConnectionCommand toCommand(Long userId) {
        return new CreateBrokerConnectionCommand(userId, brokerId, loginId, password);
    }
}
