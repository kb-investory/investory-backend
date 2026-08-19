package com.investory.auth.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeJournalCleanupPort implements JournalCleanupPort {

    private final List<Long> calledForUserIds = new ArrayList<>();

    @Override
    public void deleteAllJournals(Long userId) {
        calledForUserIds.add(userId);
    }

    public List<Long> calledForUserIds() {
        return calledForUserIds;
    }
}
