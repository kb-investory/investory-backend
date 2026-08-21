package com.investory.notification.domain.services;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.exception.NotificationErrorCode;
import com.investory.notification.domain.exception.NotificationException;
import com.investory.notification.domain.model.Notification;
import com.investory.notification.domain.model.NotificationSettings;
import com.investory.notification.domain.repositories.NotificationRepository;
import com.investory.notification.domain.repositories.NotificationSettingsRepository;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import com.investory.notification.domain.services.dto.command.MarkNotificationReadCommand;
import com.investory.notification.domain.services.dto.query.GetNotificationDetailQuery;
import com.investory.notification.domain.services.dto.query.GetNotificationsQuery;
import com.investory.notification.domain.services.dto.result.MarkAllNotificationsReadResult;
import com.investory.notification.domain.services.dto.result.MarkNotificationReadResult;
import com.investory.notification.domain.services.dto.result.NotificationListResult;
import com.investory.notification.domain.services.dto.result.NotificationResult;
import com.investory.notification.domain.services.dto.result.UnreadCountResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                NotificationSettingsRepository notificationSettingsRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
    }

    public NotificationListResult getNotifications(GetNotificationsQuery query) {
        if (query.page() < 0 || query.size() < 1) {
            throw new NotificationException(NotificationErrorCode.INVALID_PAGE_PARAMS);
        }

        int offset = query.page() * query.size();
        List<NotificationResult> content = notificationRepository
                .findByUser(query.userId(), query.isRead(), offset, query.size())
                .stream()
                .map(NotificationResult::from)
                .collect(Collectors.toList());
        long totalElements = notificationRepository.countByUser(query.userId(), query.isRead());
        // 뱃지용 unreadCount는 현재 조회 필터와 무관하게 항상 전체 안읽음 기준이다.
        long unreadCount = notificationRepository.countByUser(query.userId(), false);

        return new NotificationListResult(content, query.page(), query.size(), totalElements, unreadCount);
    }

    public NotificationResult getNotification(GetNotificationDetailQuery query) {
        Notification notification = findOwnedNotification(query.notificationId(), query.userId());
        return NotificationResult.from(notification);
    }

    public MarkNotificationReadResult markAsRead(MarkNotificationReadCommand command) {
        Notification notification = findOwnedNotification(command.notificationId(), command.userId());
        Notification updated = notification.markAsRead(Instant.now());
        if (updated != notification) {
            notificationRepository.update(updated);
        }
        return new MarkNotificationReadResult(updated.getNotificationId(), updated.isRead(), updated.getReadAt());
    }

    // 안읽은 알림만 대상으로 일괄 읽음처리한다("모두 읽음" 버튼). 이미 읽은 알림의 readAt은 그대로 둔다.
    public MarkAllNotificationsReadResult markAllAsRead(Long userId) {
        Instant now = Instant.now();
        int updatedCount = notificationRepository.markAllAsRead(userId, now);
        return new MarkAllNotificationsReadResult(updatedCount, now);
    }

    // 헤더/탭 뱃지용 — 목록을 이미 불러온 화면은 목록 응답의 unreadCount로 대체하고 별도 호출할 필요 없다.
    public UnreadCountResult getUnreadCount(Long userId) {
        return new UnreadCountResult(notificationRepository.countByUser(userId, false));
    }

    // asset/tendency/simulation의 이벤트 리스너(infra/listeners)에서만 호출된다. "알림을 보낼지"는
    // 발행 측이 아니라 여기서 판단한다(CLAUDE.md §8-2) — 설정 행이 아직 없는 사용자는 기본값(전부 수신)으로 간주한다.
    public void createIfEnabled(CreateNotificationCommand command) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(command.userId())
                .orElseGet(() -> NotificationSettings.defaults(command.userId()));
        if (!isEnabled(settings, command.notificationType())) {
            return;
        }
        Notification notification = Notification.create(
                command.userId(), command.notificationType(), command.title(), command.message(), command.referenceId());
        notificationRepository.save(notification);
    }

    // auth.domain.ports.NotificationCleanupPort 구현체(별도 작업)에서 호출 예정 — 계정 탈퇴 시
    // 사용자의 알림·알림 설정을 전부 지운다.
    public void deleteAllForUser(Long userId) {
        notificationRepository.deleteByUserId(userId);
        notificationSettingsRepository.deleteByUserId(userId);
    }

    private boolean isEnabled(NotificationSettings settings, NotificationType type) {
        return switch (type) {
            case TRADE_INGESTED -> settings.isTradeIngestedEnabled();
            case TENDENCY_ANALYZED -> settings.isTendencyAnalyzedEnabled();
            case SIMULATION_COMPLETED -> settings.isSimulationCompletedEnabled();
        };
    }

    private Notification findNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    // notificationId로 조회한 뒤 요청한 userId 소유인지까지 확인한다 — 존재 여부와 소유권 판단을 한 곳에 모은다.
    private Notification findOwnedNotification(Long notificationId, Long userId) {
        Notification notification = findNotification(notificationId);
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }
}
