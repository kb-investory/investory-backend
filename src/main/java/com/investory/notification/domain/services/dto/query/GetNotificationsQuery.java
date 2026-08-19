package com.investory.notification.domain.services.dto.query;

// isRead가 null이면 전체 조회, true/false면 해당 상태만 필터링한다.
public record GetNotificationsQuery(
        Long userId,
        Boolean isRead,
        int page,
        int size
) {
}
