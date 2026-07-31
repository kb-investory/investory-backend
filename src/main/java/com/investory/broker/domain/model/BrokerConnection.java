package com.investory.broker.domain.model;

import com.investory.broker.domain.constant.ConnectionStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class BrokerConnection {

    private final Long connectionId;
    private final Long brokerId;
    private final String brokerCode;
    private final String brokerName;
    private final ConnectionStatus connectionStatus;
    private final Instant connectedAt;
    private final Instant lastSyncedAt;
    private final int accountCount;

    private BrokerConnection(
            Long connectionId,
            Long brokerId,
            String brokerCode,
            String brokerName,
            ConnectionStatus connectionStatus,
            Instant connectedAt,
            Instant lastSyncedAt,
            int accountCount) {
        this.connectionId = connectionId;
        this.brokerId = brokerId;
        this.brokerCode = brokerCode;
        this.brokerName = brokerName;
        this.connectionStatus = connectionStatus;
        this.connectedAt = connectedAt;
        this.lastSyncedAt = lastSyncedAt;
        this.accountCount = accountCount;
    }

    public static BrokerConnection of(
            Long connectionId,
            Long brokerId,
            String brokerCode,
            String brokerName,
            ConnectionStatus connectionStatus,
            Instant connectedAt,
            Instant lastSyncedAt,
            int accountCount) {
        return new BrokerConnection(
                connectionId, brokerId, brokerCode, brokerName,
                connectionStatus, connectedAt, lastSyncedAt, accountCount
        );
    }
}