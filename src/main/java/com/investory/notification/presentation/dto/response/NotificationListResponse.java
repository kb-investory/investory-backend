package com.investory.notification.presentation.dto.response;

import com.investory.notification.domain.services.dto.result.NotificationListResult;

import java.util.List;
import java.util.stream.Collectors;

public record NotificationListResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        long unreadCount
) {
    public static NotificationListResponse from(NotificationListResult result) {
        List<NotificationResponse> content = result.content().stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return new NotificationListResponse(content, result.page(), result.size(), result.totalElements(), result.unreadCount());
    }
}
