package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.services.dto.result.DisconnectBrokerConnectionResult;

public record DisconnectBrokerConnectionResponse(
    Long connectionId,
    String connectionStatus
) {
    public static DisconnectBrokerConnectionResponse from(DisconnectBrokerConnectionResult result) {
        return new DisconnectBrokerConnectionResponse(result.connectionId(), result.connectionStatus().name());
    }
}
