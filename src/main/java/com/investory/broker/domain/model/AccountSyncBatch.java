package com.investory.broker.domain.model;

import com.investory.broker.domain.constant.SyncStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class AccountSyncBatch {

    private final Long syncBatchId;
    private final Long connectionId;
    private final SyncStatus syncStatus;
    private final Instant requestedAt;
    private final Instant completedAt;
    private final String errorMessage;

    private AccountSyncBatch(
            Long syncBatchId,
            Long connectionId,
            SyncStatus syncStatus,
            Instant requestedAt,
            Instant completedAt,
            String errorMessage) {
        this.syncBatchId = syncBatchId;
        this.connectionId = connectionId;
        this.syncStatus = syncStatus;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
    }

    public static AccountSyncBatch of(
            Long syncBatchId,
            Long connectionId,
            SyncStatus syncStatus,
            Instant requestedAt,
            Instant completedAt,
            String errorMessage) {
        return new AccountSyncBatch(syncBatchId, connectionId, syncStatus, requestedAt, completedAt, errorMessage);
    }
}
