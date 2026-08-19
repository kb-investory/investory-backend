package com.investory.notification.presentation.dto.response;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.services.dto.result.NotificationResult;

import java.time.Instant;

public record NotificationResponse(
        Long notificationId,
        NotificationType notificationType,
        String title,
        String message,
        Long referenceId,
        boolean isRead,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.notificationId(),
                result.notificationType(),
                result.title(),
                result.message(),
                result.referenceId(),
                result.isRead(),
                result.createdAt(),
                result.readAt()
        );
    }
}
