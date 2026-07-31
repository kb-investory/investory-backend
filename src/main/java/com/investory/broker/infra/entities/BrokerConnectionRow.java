package com.investory.broker.infra.entities;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class BrokerConnectionRow {
    private Long connectionId;
    private Long brokerId;
    private String brokerCode;
    private String brokerName;
    private String connectionStatus;
    private Instant connectedAt;
    private Instant lastSyncedAt;
    private int accountCount;

    public BrokerConnection toDomain() {
        return BrokerConnection.of(
                connectionId,
                brokerId,
                brokerCode,
                brokerName,
                ConnectionStatus.valueOf(connectionStatus),
                connectedAt,
                lastSyncedAt,
                accountCount
        );
    }
}
