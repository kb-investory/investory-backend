package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.services.dto.result.SyncConnectionResult;

import java.time.Instant;

public record SyncConnectionResponse(
    Long syncBatchId,
    Long connectionId,
    SyncStatus syncStatus,
    Instant requestedAt,
    Instant completedAt,
    int accountCount,
    int insertedTradeCount,
    int skippedTradeCount,
    int holdingCount,
    String errorMessage
) {
    public static SyncConnectionResponse from(SyncConnectionResult result) {
        return new SyncConnectionResponse(
                result.syncBatchId(),
                result.connectionId(),
                result.syncStatus(),
                result.requestedAt(),
                result.completedAt(),
                result.accountCount(),
                result.insertedTradeCount(),
                result.skippedTradeCount(),
                result.holdingCount(),
                result.errorMessage()
        );
    }
}
