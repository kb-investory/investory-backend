package com.investory.notification.domain.repositories;

import com.investory.notification.domain.model.NotificationSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeNotificationSettingsRepository implements NotificationSettingsRepository {

    private final Map<Long, NotificationSettings> settingsByUserId = new HashMap<>();

    public void add(NotificationSettings settings) {
        settingsByUserId.put(settings.getUserId(), settings);
    }

    @Override
    public Optional<NotificationSettings> findByUserId(Long userId) {
        return Optional.ofNullable(settingsByUserId.get(userId));
    }

    @Override
    public void upsert(NotificationSettings settings) {
        settingsByUserId.put(settings.getUserId(), settings);
    }

    @Override
    public void deleteByUserId(Long userId) {
        settingsByUserId.remove(userId);
    }
}
