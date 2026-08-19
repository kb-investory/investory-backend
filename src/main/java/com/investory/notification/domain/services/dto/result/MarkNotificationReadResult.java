package com.investory.notification.domain.services.dto.result;

import java.time.Instant;

public record MarkNotificationReadResult(Long notificationId, boolean isRead, Instant readAt) {
}
