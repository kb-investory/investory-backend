package com.investory.notification.infra.entities;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.model.Notification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRow {
    private Long notificationId;
    private Long userId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private Long referenceId;
    private boolean read;
    private Instant createdAt;
    private Instant readAt;

    public Notification toDomain() {
        return Notification.of(notificationId, userId, notificationType, title, message, referenceId, read, createdAt, readAt);
    }

    public static NotificationRow from(Notification notification) {
        NotificationRow row = new NotificationRow();
        row.notificationId = notification.getNotificationId();
        row.userId = notification.getUserId();
        row.notificationType = notification.getNotificationType();
        row.title = notification.getTitle();
        row.message = notification.getMessage();
        row.referenceId = notification.getReferenceId();
        row.read = notification.isRead();
        row.createdAt = notification.getCreatedAt();
        row.readAt = notification.getReadAt();
        return row;
    }
}
