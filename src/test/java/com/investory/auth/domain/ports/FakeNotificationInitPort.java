package com.investory.auth.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeNotificationInitPort implements NotificationInitPort {

    private final List<Long> calledForUserIds = new ArrayList<>();

    @Override
    public void initSettings(Long userId) {
        calledForUserIds.add(userId);
    }

    public List<Long> calledForUserIds() {
        return calledForUserIds;
    }
}
