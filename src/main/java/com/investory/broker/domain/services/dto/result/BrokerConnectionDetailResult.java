package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.model.BrokerConnection;

import java.time.Instant;

public record BrokerConnectionDetailResult(
    Long connectionId,
    Long brokerId,
    String brokerCode,
    String brokerName,
    ConnectionStatus connectionStatus,
    Instant connectedAt,
    Instant lastSyncedAt,
    LatestSync latestSync
) {
    public static BrokerConnectionDetailResult of(BrokerConnection connection, AccountSyncBatch latestSyncBatch) {
        return new BrokerConnectionDetailResult(
                connection.getConnectionId(),
                connection.getBrokerId(),
                connection.getBrokerCode(),
                connection.getBrokerName(),
                connection.getConnectionStatus(),
                connection.getConnectedAt(),
                connection.getLastSyncedAt(),
                latestSyncBatch == null ? null : LatestSync.from(latestSyncBatch)
        );
    }

    public record LatestSync(
        Long syncBatchId,
        SyncStatus syncStatus,
        Instant requestedAt,
        Instant completedAt,
        String errorMessage
    ) {
        public static LatestSync from(AccountSyncBatch batch) {
            return new LatestSync(
                    batch.getSyncBatchId(),
                    batch.getSyncStatus(),
                    batch.getRequestedAt(),
                    batch.getCompletedAt(),
                    batch.getErrorMessage()
            );
        }
    }
}
