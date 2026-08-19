package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.SyncStatus;
import com.investory.broker.domain.model.AccountSyncBatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FakeAccountSyncBatchRepository implements AccountSyncBatchRepository {

    private final List<AccountSyncBatch> batches = new ArrayList<>();
    private long nextBatchId = 1L;

    public void add(AccountSyncBatch batch) {
        batches.add(batch);
    }

    @Override
    public Long create(Long connectionId) {
        Long batchId = nextBatchId++;
        batches.add(AccountSyncBatch.of(batchId, connectionId, SyncStatus.REQUESTED, Instant.now(), null, null));
        return batchId;
    }

    @Override
    public void markSuccess(Long syncBatchId) {
        replace(syncBatchId, batch -> AccountSyncBatch.of(
                batch.getSyncBatchId(), batch.getConnectionId(), SyncStatus.SUCCESS,
                batch.getRequestedAt(), Instant.now(), null));
    }

    @Override
    public void markFailed(Long syncBatchId, String errorMessage) {
        replace(syncBatchId, batch -> AccountSyncBatch.of(
                batch.getSyncBatchId(), batch.getConnectionId(), SyncStatus.FAILED,
                batch.getRequestedAt(), Instant.now(), errorMessage));
    }

    @Override
    public Optional<AccountSyncBatch> findLatestByConnectionId(Long connectionId) {
        return batches.stream()
                .filter(batch -> batch.getConnectionId().equals(connectionId))
                .max(Comparator.comparing(AccountSyncBatch::getRequestedAt));
    }

    @Override
    public boolean existsInProgress(Long connectionId) {
        return batches.stream()
                .anyMatch(batch -> batch.getConnectionId().equals(connectionId) && batch.getSyncStatus() == SyncStatus.REQUESTED);
    }

    private void replace(Long syncBatchId, java.util.function.UnaryOperator<AccountSyncBatch> updater) {
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).getSyncBatchId().equals(syncBatchId)) {
                batches.set(i, updater.apply(batches.get(i)));
                return;
            }
        }
    }
}
