package com.investory.notification.presentation.controller;

import com.investory.notification.domain.services.NotificationService;
import com.investory.notification.domain.services.dto.command.MarkNotificationReadCommand;
import com.investory.notification.domain.services.dto.query.GetNotificationDetailQuery;
import com.investory.notification.domain.services.dto.query.GetNotificationsQuery;
import com.investory.notification.domain.services.dto.result.MarkNotificationReadResult;
import com.investory.notification.domain.services.dto.result.NotificationListResult;
import com.investory.notification.domain.services.dto.result.NotificationResult;
import com.investory.notification.presentation.dto.response.NotificationListResponse;
import com.investory.notification.presentation.dto.response.NotificationReadResponse;
import com.investory.notification.presentation.dto.response.NotificationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponse getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        NotificationListResult result = notificationService.getNotifications(
                new GetNotificationsQuery(userId, isRead, page, size));
        return NotificationListResponse.from(result);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {
        NotificationResult result = notificationService.getNotification(new GetNotificationDetailQuery(userId, notificationId));
        return NotificationResponse.from(result);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationReadResponse markAsRead(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {
        MarkNotificationReadResult result = notificationService.markAsRead(
                new MarkNotificationReadCommand(userId, notificationId));
        return NotificationReadResponse.from(result);
    }
}
