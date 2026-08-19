package com.investory.notification.domain.services.dto.result;

import java.util.List;

public record NotificationListResult(
        List<NotificationResult> content,
        int page,
        int size,
        long totalElements,
        long unreadCount
) {
}
