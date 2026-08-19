package com.investory.notification.infra.port_impls;

import com.investory.auth.domain.ports.NotificationCleanupPort;
import com.investory.notification.domain.services.NotificationService;
import org.springframework.stereotype.Component;

// auth.domain.ports를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 서비스 호출로 위임한다(§5).
@Component
public class NotificationCleanupPortImpl implements NotificationCleanupPort {

    private final NotificationService notificationService;

    public NotificationCleanupPortImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void deleteAllNotifications(Long userId) {
        notificationService.deleteAllForUser(userId);
    }
}
