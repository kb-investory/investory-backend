package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.SyncStatus;

import java.time.Instant;

public record SyncConnectionResult(
    Long syncBatchId,
    Long connectionId,
    SyncStatus syncStatus,
    Instant requestedAt,
    Instant completedAt,
    int accountCount,
    int insertedTradeCount,
    int skippedTradeCount,
    int holdingCount
) {
}
