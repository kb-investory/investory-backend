package com.investory.notification.domain.repositories;

import com.investory.notification.domain.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    // isRead가 null이면 전체, true/false면 해당 상태만 필터링한다.
    List<Notification> findByUser(Long userId, Boolean isRead, int offset, int limit);

    // isRead가 null이면 전체 건수, true/false면 해당 상태의 건수. unreadCount 조회 시 false로 호출한다.
    long countByUser(Long userId, Boolean isRead);

    Optional<Notification> findById(Long notificationId);

    Notification save(Notification notification);

    void update(Notification notification);

    // 안읽은 알림만 대상으로 일괄 읽음처리한다. 이미 읽은 알림은 건드리지 않는다(readAt 유지).
    // 반환값은 실제로 갱신된 건수.
    int markAllAsRead(Long userId, Instant readAt);

    // auth.domain.ports.NotificationCleanupPort 구현체에서만 호출 — 계정 탈퇴 시 사용자의 알림을 전부 지운다.
    void deleteByUserId(Long userId);
}
