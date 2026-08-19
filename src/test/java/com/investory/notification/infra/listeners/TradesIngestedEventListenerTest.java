package com.investory.notification.infra.listeners;

import com.investory.ledger.domain.events.TradesIngestedEvent;
import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.model.Notification;
import com.investory.notification.domain.repositories.FakeNotificationRepository;
import com.investory.notification.domain.repositories.FakeNotificationSettingsRepository;
import com.investory.notification.domain.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradesIngestedEventListenerTest {

    private static final Long USER_ID = 100L;
    private static final Long ACCOUNT_ID = 11L;

    private FakeNotificationRepository notificationRepository;
    private TradesIngestedEventListener listener;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        NotificationService notificationService = new NotificationService(notificationRepository, new FakeNotificationSettingsRepository());
        listener = new TradesIngestedEventListener(notificationService);
    }

    @Test
    void 이벤트를_받으면_거래적재_알림을_생성한다() {
        listener.handle(new TradesIngestedEvent(USER_ID, ACCOUNT_ID, 3));

        List<Notification> saved = notificationRepository.findByUser(USER_ID, null, 0, 10);
        assertEquals(1, saved.size());
        assertEquals(NotificationType.TRADE_INGESTED, saved.get(0).getNotificationType());
        assertEquals(ACCOUNT_ID, saved.get(0).getReferenceId());
    }

    // TendencyAnalyzedEventListenerTest와 같은 이유로 애너테이션 자체가 남아있는지만 지켜서,
    // 누군가 무심코 지웠을 때 동기화 흐름이 다시 알림 생성을 물고 늘어지는 회귀를 잡는다.
    @Test
    void handle은_Async로_호출자_흐름에서_분리되어_있다() throws NoSuchMethodException {
        Method handle = TradesIngestedEventListener.class.getMethod("handle", TradesIngestedEvent.class);
        assertNotNull(handle.getAnnotation(Async.class));
    }
}
