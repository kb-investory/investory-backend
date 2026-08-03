package com.investory.broker.domain.repositories;

import java.util.HashMap;
import java.util.Map;

public class FakeAccountSyncBatchRepository implements AccountSyncBatchRepository {

    public enum Status { REQUESTED, SUCCESS, FAILED }

    public final Map<Long, Status> statusByBatchId = new HashMap<>();
    public final Map<Long, String> errorMessageByBatchId = new HashMap<>();
    private long nextBatchId = 1L;

    @Override
    public Long create(Long connectionId) {
        Long batchId = nextBatchId++;
        statusByBatchId.put(batchId, Status.REQUESTED);
        return batchId;
    }

    @Override
    public void markSuccess(Long syncBatchId) {
        statusByBatchId.put(syncBatchId, Status.SUCCESS);
    }

    @Override
    public void markFailed(Long syncBatchId, String errorMessage) {
        statusByBatchId.put(syncBatchId, Status.FAILED);
        errorMessageByBatchId.put(syncBatchId, errorMessage);
    }
}
