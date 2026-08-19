package com.investory.notification.domain.model;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.exception.NotificationErrorCode;
import com.investory.notification.domain.exception.NotificationException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class Notification {

    private final Long notificationId;
    private final Long userId;
    private final NotificationType notificationType;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final boolean read;
    private final Instant createdAt;
    private final Instant readAt;

    private Notification(Long notificationId, Long userId, NotificationType notificationType, String title,
                          String message, Long referenceId, boolean read, Instant createdAt, Instant readAt) {
        requireNonNull(userId);
        requireNonNull(notificationType);
        requireNonNull(title);
        requireNonNull(message);
        requireNonNull(createdAt);

        this.notificationId = notificationId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DATA);
        }
    }

    public static Notification create(Long userId, NotificationType notificationType, String title, String message, Long referenceId) {
        return new Notification(null, userId, notificationType, title, message, referenceId, false, Instant.now(), null);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용).
    public static Notification of(Long notificationId, Long userId, NotificationType notificationType, String title,
                                   String message, Long referenceId, boolean read, Instant createdAt, Instant readAt) {
        return new Notification(notificationId, userId, notificationType, title, message, referenceId, read, createdAt, readAt);
    }

    // 이미 읽음이면 그대로 반환한다 — readAt을 덮어쓰지 않는 멱등 처리(알림 API 명세 §3 정책).
    public Notification markAsRead(Instant now) {
        if (read) {
            return this;
        }
        return new Notification(notificationId, userId, notificationType, title, message, referenceId, true, createdAt, now);
    }
}
