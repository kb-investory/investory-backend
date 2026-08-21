package com.investory.notification.presentation.dto.response;

import com.investory.notification.domain.services.dto.result.UnreadCountResult;

public record UnreadCountResponse(long unreadCount) {
    public static UnreadCountResponse from(UnreadCountResult result) {
        return new UnreadCountResponse(result.unreadCount());
    }
}
