package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;

import java.time.Instant;

public record BrokerConnectionResponse(
    Long connectionId,
    Long brokerId,
    String brokerCode,
    String brokerName,
    ConnectionStatus connectionStatus,
    Instant connectedAt,
    Instant lastSyncedAt,
    int accountCount
) {
    public static BrokerConnectionResponse from(BrokerConnectionResult result) {
        return new BrokerConnectionResponse(
                result.connectionId(),
                result.brokerId(),
                result.brokerCode(),
                result.brokerName(),
                result.connectionStatus(),
                result.connectedAt(),
                result.lastSyncedAt(),
                result.accountCount()
        );
    }
}