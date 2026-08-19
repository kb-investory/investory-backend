package com.investory.auth.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeTendencyCleanupPort implements TendencyCleanupPort {

    private final List<Long> calledForUserIds = new ArrayList<>();

    @Override
    public void deleteAllAnalyses(Long userId) {
        calledForUserIds.add(userId);
    }

    public List<Long> calledForUserIds() {
        return calledForUserIds;
    }
}
