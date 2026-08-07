package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;

import java.time.Instant;

public record CreateBrokerConnectionResult(
    Long connectionId,
    Long brokerId,
    String brokerCode,
    String brokerName,
    ConnectionStatus connectionStatus,
    Instant connectedAt,
    Instant lastSyncedAt,
    SyncResult syncResult
) {
    public record SyncResult(
        Long syncBatchId,
        SyncStatus syncStatus,
        int accountCount,
        int insertedTradeCount,
        int holdingCount
    ) {
    }
}
