package com.investory.notification.presentation.dto.response;

import com.investory.notification.domain.services.dto.result.MarkAllNotificationsReadResult;

import java.time.Instant;

public record NotificationReadAllResponse(int updatedCount, Instant readAt) {
    public static NotificationReadAllResponse from(MarkAllNotificationsReadResult result) {
        return new NotificationReadAllResponse(result.updatedCount(), result.readAt());
    }
}
