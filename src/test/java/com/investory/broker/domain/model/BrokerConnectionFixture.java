package com.investory.broker.domain.model;

import com.investory.broker.domain.constant.ConnectionStatus;

import java.time.Instant;

public class BrokerConnectionFixture {

    public static BrokerConnection connection(
            Long connectionId, Long brokerId, String brokerCode, String brokerName,
            ConnectionStatus connectionStatus, Instant connectedAt, Instant lastSyncedAt, int accountCount) {
        return BrokerConnection.of(
                connectionId, brokerId, brokerCode, brokerName,
                connectionStatus, connectedAt, lastSyncedAt, accountCount
        );
    }

    public static BrokerConnection connected(Long connectionId, Long brokerId, String brokerCode, String brokerName) {
        return connection(
                connectionId, brokerId, brokerCode, brokerName,
                ConnectionStatus.CONNECTED, Instant.parse("2026-07-29T13:40:00Z"), null, 2
        );
    }
}
