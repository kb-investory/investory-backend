package com.investory.broker.domain.repositories;

public interface AccountSyncBatchRepository {
    Long create(Long connectionId);
    void markSuccess(Long syncBatchId);
    void markFailed(Long syncBatchId, String errorMessage);
}
