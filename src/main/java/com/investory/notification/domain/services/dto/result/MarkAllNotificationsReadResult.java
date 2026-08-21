package com.investory.notification.domain.services.dto.result;

import java.time.Instant;

public record MarkAllNotificationsReadResult(int updatedCount, Instant readAt) {
}
