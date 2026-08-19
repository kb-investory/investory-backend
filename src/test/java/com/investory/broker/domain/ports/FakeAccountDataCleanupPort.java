package com.investory.broker.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeAccountDataCleanupPort implements AccountDataCleanupPort {

    private final List<Long> deletedAccountIds = new ArrayList<>();

    @Override
    public void deleteAccountData(Long accountId) {
        deletedAccountIds.add(accountId);
    }

    public List<Long> deletedAccountIds() {
        return deletedAccountIds;
    }
}
