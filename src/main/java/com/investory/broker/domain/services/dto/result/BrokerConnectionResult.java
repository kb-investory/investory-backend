package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnection;

import java.time.Instant;

public record BrokerConnectionResult(
    Long connectionId,
    Long brokerId,
    String brokerCode,
    String brokerName,
    ConnectionStatus connectionStatus,
    Instant connectedAt,
    Instant lastSyncedAt,
    int accountCount
) {
    public static BrokerConnectionResult from(BrokerConnection connection) {
        return new BrokerConnectionResult(
                connection.getConnectionId(),
                connection.getBrokerId(),
                connection.getBrokerCode(),
                connection.getBrokerName(),
                connection.getConnectionStatus(),
                connection.getConnectedAt(),
                connection.getLastSyncedAt(),
                connection.getAccountCount()
        );
    }
}