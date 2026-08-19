package com.investory.notification.domain.services.dto.command;

import com.investory.notification.domain.constant.NotificationType;

// asset/tendency/simulation의 이벤트 리스너(infra/listeners)가 자기 이벤트를 이 Command로 변환해 넘긴다.
public record CreateNotificationCommand(
        Long userId,
        NotificationType notificationType,
        String title,
        String message,
        Long referenceId
) {
}
