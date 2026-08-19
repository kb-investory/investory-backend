package com.investory.notification.presentation.controller;

import com.investory.notification.domain.services.NotificationSettingsService;
import com.investory.notification.domain.services.dto.result.NotificationSettingsResult;
import com.investory.notification.presentation.dto.request.UpdateNotificationSettingsRequest;
import com.investory.notification.presentation.dto.response.NotificationSettingsResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// URL은 /users/me 하위지만 데이터·컨트롤러는 notification 패키지 소유 — 데이터 소유권 기준으로
// 패키지를 정하는 편이 나중에 분리할 때 유리하다(CLAUDE.md §3-①).
@RestController
@RequestMapping("/users/me/notification-settings")
public class NotificationSettingController {

    private final NotificationSettingsService notificationSettingsService;

    public NotificationSettingController(NotificationSettingsService notificationSettingsService) {
        this.notificationSettingsService = notificationSettingsService;
    }

    @GetMapping
    public NotificationSettingsResponse getSettings(@AuthenticationPrincipal Long userId) {
        NotificationSettingsResult result = notificationSettingsService.getSettings(userId);
        return NotificationSettingsResponse.from(result);
    }

    @PutMapping
    public NotificationSettingsResponse updateSettings(@AuthenticationPrincipal Long userId,
                                                         @RequestBody UpdateNotificationSettingsRequest request) {
        NotificationSettingsResult result = notificationSettingsService.updateSettings(request.toCommand(userId));
        return NotificationSettingsResponse.from(result);
    }
}
