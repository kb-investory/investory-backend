package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.AccountSyncBatch;

import java.util.Optional;

public interface AccountSyncBatchRepository {
    Long create(Long connectionId);
    void markSuccess(Long syncBatchId);
    void markFailed(Long syncBatchId, String errorMessage);
    Optional<AccountSyncBatch> findLatestByConnectionId(Long connectionId);
}
