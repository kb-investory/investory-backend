package com.investory.notification.domain.repositories;

import com.investory.notification.domain.model.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeNotificationRepository implements NotificationRepository {

    private final List<Notification> notifications = new ArrayList<>();
    private long nextId = 1L;

    public void add(Notification... notifications) {
        this.notifications.addAll(List.of(notifications));
    }

    @Override
    public List<Notification> findByUser(Long userId, Boolean isRead, int offset, int limit) {
        List<Notification> filtered = notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .filter(n -> isRead == null || n.isRead() == isRead)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        int from = Math.min(offset, filtered.size());
        int to = Math.min(offset + limit, filtered.size());
        return filtered.subList(from, to);
    }

    @Override
    public long countByUser(Long userId, Boolean isRead) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .filter(n -> isRead == null || n.isRead() == isRead)
                .count();
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        return notifications.stream()
                .filter(n -> n.getNotificationId().equals(notificationId))
                .findFirst();
    }

    @Override
    public Notification save(Notification notification) {
        Notification saved = Notification.of(
                nextId++,
                notification.getUserId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
        notifications.add(saved);
        return saved;
    }

    @Override
    public void update(Notification notification) {
        notifications.removeIf(existing -> existing.getNotificationId().equals(notification.getNotificationId()));
        notifications.add(notification);
    }

    @Override
    public void deleteByUserId(Long userId) {
        notifications.removeIf(n -> n.getUserId().equals(userId));
    }
}
