package com.investory.notification.domain.services.dto.result;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.model.Notification;

import java.time.Instant;

public record NotificationResult(
        Long notificationId,
        NotificationType notificationType,
        String title,
        String message,
        Long referenceId,
        boolean isRead,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
