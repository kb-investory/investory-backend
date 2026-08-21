package com.investory.notification.domain.services;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.exception.NotificationErrorCode;
import com.investory.notification.domain.exception.NotificationException;
import com.investory.notification.domain.model.Notification;
import com.investory.notification.domain.model.NotificationSettings;
import com.investory.notification.domain.repositories.FakeNotificationRepository;
import com.investory.notification.domain.repositories.FakeNotificationSettingsRepository;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import com.investory.notification.domain.services.dto.command.MarkNotificationReadCommand;
import com.investory.notification.domain.services.dto.query.GetNotificationDetailQuery;
import com.investory.notification.domain.services.dto.query.GetNotificationsQuery;
import com.investory.notification.domain.services.dto.result.MarkNotificationReadResult;
import com.investory.notification.domain.services.dto.result.NotificationListResult;
import com.investory.notification.domain.services.dto.result.NotificationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {

    private static final Long USER_ID = 1L;

    @Test
    void 목록_조회하면_unreadCount는_현재_필터와_무관하게_전체_안읽음_기준이다() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, USER_ID, NotificationType.TRADE_INGESTED, "제목1", "내용1", 10L, false, Instant.now(), null));
        repository.add(Notification.of(2L, USER_ID, NotificationType.TENDENCY_ANALYZED, "제목2", "내용2", 20L, true, Instant.now(), Instant.now()));
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());

        NotificationListResult result = service.getNotifications(new GetNotificationsQuery(USER_ID, true, 0, 20));

        assertEquals(1, result.content().size());
        assertEquals(1, result.unreadCount());
    }

    @Test
    void 페이지가_음수면_INVALID_PAGE_PARAMS_예외를_던진다() {
        NotificationService service = new NotificationService(new FakeNotificationRepository(), new FakeNotificationSettingsRepository());

        NotificationException exception = assertThrows(NotificationException.class,
                () -> service.getNotifications(new GetNotificationsQuery(USER_ID, null, -1, 20)));
        assertEquals(NotificationErrorCode.INVALID_PAGE_PARAMS, exception.getErrorCode());
    }

    @Test
    void 본인_소유_알림을_단건_조회할_수_있다() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());

        NotificationResult result = service.getNotification(new GetNotificationDetailQuery(USER_ID, 1L));

        assertEquals("제목", result.title());
    }

    @Test
    void 남의_알림을_단건_조회하면_NOTIFICATION_NOT_FOUND_예외를_던진다() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, 999L, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());

        NotificationException exception = assertThrows(NotificationException.class,
                () -> service.getNotification(new GetNotificationDetailQuery(USER_ID, 1L)));
        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 본인_소유_알림을_읽음처리하면_isRead가_true가_된다() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());

        MarkNotificationReadResult result = service.markAsRead(new MarkNotificationReadCommand(USER_ID, 1L));

        assertTrue(result.isRead());
    }

    @Test
    void 이미_읽은_알림을_다시_읽음처리해도_기존_readAt을_유지한채_200으로_처리된다() {
        Instant firstReadAt = Instant.now().minusSeconds(60);
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, true, Instant.now(), firstReadAt));
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());

        MarkNotificationReadResult result = service.markAsRead(new MarkNotificationReadCommand(USER_ID, 1L));

        assertEquals(firstReadAt, result.readAt());
    }

    @Test
    void 남의_알림을_읽음처리하면_NOTIFICATION_NOT_FOUND_예외를_던진다() {
        FakeNotificationRepository repository = new FakeNotificationRepository();
        repository.add(Notification.of(1L, 999L, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        NotificationService service = new NotificationService(repository, new FakeNotificationSettingsRepository());

        NotificationException exception = assertThrows(NotificationException.class,
                () -> service.markAsRead(new MarkNotificationReadCommand(USER_ID, 1L)));
        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 수신설정이_꺼져있으면_알림을_생성하지_않는다() {
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        FakeNotificationSettingsRepository settingsRepository = new FakeNotificationSettingsRepository();
        settingsRepository.add(NotificationSettings.defaults(USER_ID).update(false, true, true));
        NotificationService service = new NotificationService(notificationRepository, settingsRepository);

        service.createIfEnabled(new CreateNotificationCommand(USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L));

        assertEquals(0, notificationRepository.countByUser(USER_ID, null));
    }

    @Test
    void 설정_행이_없는_사용자는_기본값_전부_수신으로_간주해_알림을_생성한다() {
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        NotificationService service = new NotificationService(notificationRepository, new FakeNotificationSettingsRepository());

        service.createIfEnabled(new CreateNotificationCommand(USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L));

        assertEquals(1, notificationRepository.countByUser(USER_ID, null));
    }

    @Test
    void 수신설정이_켜져있으면_알림을_생성한다() {
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        FakeNotificationSettingsRepository settingsRepository = new FakeNotificationSettingsRepository();
        settingsRepository.add(NotificationSettings.defaults(USER_ID));
        NotificationService service = new NotificationService(notificationRepository, settingsRepository);

        service.createIfEnabled(new CreateNotificationCommand(USER_ID, NotificationType.SIMULATION_COMPLETED, "제목", "내용", 30L));

        assertEquals(1, notificationRepository.countByUser(USER_ID, false));
    }

    @Test
    void deleteAllForUser는_알림과_설정을_모두_지운다() {
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        notificationRepository.add(Notification.of(1L, USER_ID, NotificationType.TRADE_INGESTED, "제목", "내용", 10L, false, Instant.now(), null));
        FakeNotificationSettingsRepository settingsRepository = new FakeNotificationSettingsRepository();
        settingsRepository.add(NotificationSettings.defaults(USER_ID));
        NotificationService service = new NotificationService(notificationRepository, settingsRepository);

        service.deleteAllForUser(USER_ID);

        assertEquals(0, notificationRepository.countByUser(USER_ID, null));
        assertFalse(settingsRepository.findByUserId(USER_ID).isPresent());
    }
}
