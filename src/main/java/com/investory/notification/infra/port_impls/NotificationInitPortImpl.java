package com.investory.notification.infra.port_impls;

import com.investory.auth.domain.ports.NotificationInitPort;
import com.investory.notification.domain.services.NotificationSettingsService;
import org.springframework.stereotype.Component;

// auth.domain.ports를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 서비스 호출로 위임한다(§5).
@Component
public class NotificationInitPortImpl implements NotificationInitPort {

    private final NotificationSettingsService notificationSettingsService;

    public NotificationInitPortImpl(NotificationSettingsService notificationSettingsService) {
        this.notificationSettingsService = notificationSettingsService;
    }

    @Override
    public void initSettings(Long userId) {
        notificationSettingsService.initDefaultSettings(userId);
    }
}
