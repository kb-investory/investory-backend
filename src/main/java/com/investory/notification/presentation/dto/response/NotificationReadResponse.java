package com.investory.notification.presentation.dto.response;

import com.investory.notification.domain.services.dto.result.MarkNotificationReadResult;

import java.time.Instant;

public record NotificationReadResponse(Long notificationId, boolean isRead, Instant readAt) {
    public static NotificationReadResponse from(MarkNotificationReadResult result) {
        return new NotificationReadResponse(result.notificationId(), result.isRead(), result.readAt());
    }
}
