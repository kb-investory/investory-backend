package com.investory.auth.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeNotificationCleanupPort implements NotificationCleanupPort {

    private final List<Long> calledForUserIds = new ArrayList<>();

    @Override
    public void deleteAllNotifications(Long userId) {
        calledForUserIds.add(userId);
    }

    public List<Long> calledForUserIds() {
        return calledForUserIds;
    }
}
