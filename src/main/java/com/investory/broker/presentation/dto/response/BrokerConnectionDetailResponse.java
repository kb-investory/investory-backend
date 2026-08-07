package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.services.dto.result.BrokerConnectionDetailResult;

import java.time.Instant;

public record BrokerConnectionDetailResponse(
    Long connectionId,
    Long brokerId,
    String brokerCode,
    String brokerName,
    ConnectionStatus connectionStatus,
    Instant connectedAt,
    Instant lastSyncedAt,
    LatestSyncResponse latestSync
) {
    public static BrokerConnectionDetailResponse from(BrokerConnectionDetailResult result) {
        return new BrokerConnectionDetailResponse(
                result.connectionId(),
                result.brokerId(),
                result.brokerCode(),
                result.brokerName(),
                result.connectionStatus(),
                result.connectedAt(),
                result.lastSyncedAt(),
                result.latestSync() == null ? null : LatestSyncResponse.from(result.latestSync())
        );
    }

    public record LatestSyncResponse(
        Long syncBatchId,
        SyncStatus syncStatus,
        Instant requestedAt,
        Instant completedAt,
        String errorMessage
    ) {
        public static LatestSyncResponse from(BrokerConnectionDetailResult.LatestSync latestSync) {
            return new LatestSyncResponse(
                    latestSync.syncBatchId(),
                    latestSync.syncStatus(),
                    latestSync.requestedAt(),
                    latestSync.completedAt(),
                    latestSync.errorMessage()
            );
        }
    }
}
