package com.investory.auth.domain.ports;

// notification.domain.services.NotificationService.deleteAllForUser(Long)로 위임 예정.
// 계정 탈퇴 시 사용자의 notifications/notification_settings를 전부 지운다.
public interface NotificationCleanupPort {
    void deleteAllNotifications(Long userId);
}
