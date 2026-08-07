package com.investory.broker.infra.entities;

import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.model.AccountSyncBatch;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AccountSyncBatchRow {
    private Long syncBatchId;
    private Long connectionId;
    private String syncStatus;
    private Instant requestedAt;
    private Instant completedAt;
    private String errorMessage;

    public AccountSyncBatch toDomain() {
        return AccountSyncBatch.of(
                syncBatchId, connectionId, SyncStatus.valueOf(syncStatus),
                requestedAt, completedAt, errorMessage
        );
    }
}
