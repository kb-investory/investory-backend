package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.ConnectionStatus;

public record DisconnectBrokerConnectionResult(
    Long connectionId,
    ConnectionStatus connectionStatus
) {
}
