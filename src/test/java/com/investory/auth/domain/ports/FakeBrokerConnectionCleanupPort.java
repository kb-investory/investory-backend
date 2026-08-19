package com.investory.auth.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeBrokerConnectionCleanupPort implements BrokerConnectionCleanupPort {

    private final List<Long> calledForUserIds = new ArrayList<>();

    @Override
    public void disconnectAllConnections(Long userId) {
        calledForUserIds.add(userId);
    }

    public List<Long> calledForUserIds() {
        return calledForUserIds;
    }
}
