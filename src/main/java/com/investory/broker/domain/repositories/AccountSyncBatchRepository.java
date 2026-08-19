package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.AccountSyncBatch;

import java.util.Optional;

public interface AccountSyncBatchRepository {
    Long create(Long connectionId);
    void markSuccess(Long syncBatchId);
    void markFailed(Long syncBatchId, String errorMessage);
    Optional<AccountSyncBatch> findLatestByConnectionId(Long connectionId);

    // 같은 connection에 대해 아직 REQUESTED 상태로 끝나지 않은 배치가 있는지 — 동시 동기화 가드용.
    boolean existsInProgress(Long connectionId);
}
