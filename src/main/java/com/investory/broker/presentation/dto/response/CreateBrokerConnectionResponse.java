package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.services.dto.result.CreateBrokerConnectionResult;

import java.time.Instant;

public record CreateBrokerConnectionResponse(
    Long connectionId,
    Long brokerId,
    String brokerCode,
    String brokerName,
    ConnectionStatus connectionStatus,
    Instant connectedAt,
    Instant lastSyncedAt,
    SyncResultResponse syncResult
) {
    public static CreateBrokerConnectionResponse from(CreateBrokerConnectionResult result) {
        return new CreateBrokerConnectionResponse(
                result.connectionId(),
                result.brokerId(),
                result.brokerCode(),
                result.brokerName(),
                result.connectionStatus(),
                result.connectedAt(),
                result.lastSyncedAt(),
                SyncResultResponse.from(result.syncResult())
        );
    }

    public record SyncResultResponse(
        Long syncBatchId,
        SyncStatus syncStatus,
        int accountCount,
        int insertedTradeCount,
        int holdingCount
    ) {
        public static SyncResultResponse from(CreateBrokerConnectionResult.SyncResult syncResult) {
            return new SyncResultResponse(
                    syncResult.syncBatchId(),
                    syncResult.syncStatus(),
                    syncResult.accountCount(),
                    syncResult.insertedTradeCount(),
                    syncResult.holdingCount()
            );
        }
    }
}
