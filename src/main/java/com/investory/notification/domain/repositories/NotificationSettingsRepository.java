package com.investory.notification.domain.repositories;

import com.investory.notification.domain.model.NotificationSettings;

import java.util.Optional;

public interface NotificationSettingsRepository {

    Optional<NotificationSettings> findByUserId(Long userId);

    // 행이 없으면 생성, 있으면 갱신 (user_id가 PK).
    void upsert(NotificationSettings settings);

    // auth.domain.ports.NotificationCleanupPort 구현체에서만 호출 — 계정 탈퇴 시.
    void deleteByUserId(Long userId);
}
