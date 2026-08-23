package com.investory.notification.infra.listeners;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.model.Notification;
import com.investory.notification.domain.repositories.FakeNotificationRepository;
import com.investory.notification.domain.repositories.FakeNotificationSettingsRepository;
import com.investory.notification.domain.services.NotificationService;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TendencyAnalyzedEventListenerTest {

    private static final Long USER_ID = 100L;
    private static final Long ANALYSIS_RUN_ID = 1L;

    private FakeNotificationRepository notificationRepository;
    private TendencyAnalyzedEventListener listener;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        NotificationService notificationService = new NotificationService(notificationRepository, new FakeNotificationSettingsRepository());
        listener = new TendencyAnalyzedEventListener(notificationService, Runnable::run);
    }

    @Test
    void 이벤트를_받으면_성향분석_완료_알림을_생성한다() {
        listener.handle(new TendencyAnalyzedEvent(USER_ID, ANALYSIS_RUN_ID, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"))));

        List<Notification> saved = notificationRepository.findByUser(USER_ID, null, 0, 10);
        assertEquals(1, saved.size());
        assertEquals(NotificationType.TENDENCY_ANALYZED, saved.get(0).getNotificationType());
        assertEquals(ANALYSIS_RUN_ID, saved.get(0).getReferenceId());
    }

    // handle()이 알림 생성을 호출 스레드에서 직접 하지 않고 실행기에 제출만 하는지 확인한다.
    @Test
    void handle은_알림_생성을_직접_실행하지_않고_실행기에_제출만_한다() {
        AtomicBoolean submitted = new AtomicBoolean(false);
        Executor recordingExecutor = command -> submitted.set(true); // 제출만 기록, 실행은 안 함
        NotificationService notificationService = new NotificationService(notificationRepository, new FakeNotificationSettingsRepository());
        TendencyAnalyzedEventListener recordingListener = new TendencyAnalyzedEventListener(notificationService, recordingExecutor);

        recordingListener.handle(new TendencyAnalyzedEvent(USER_ID, ANALYSIS_RUN_ID, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"))));

        assertTrue(submitted.get());
        assertTrue(notificationRepository.findByUser(USER_ID, null, 0, 10).isEmpty());
    }

    // #204 회귀 가드 — notificationExecutor 큐가 가득 차면 CompletableFuture 제출 자체가
    // RejectedExecutionException을 던진다. 예전엔 @Async 프록시 단계에서 이 예외가 곧장 던져져
    // publishEvent() -> AnalysisRunService.runAnalysis()까지 전파되며 이미 저장된 분석 결과를
    // 500으로 되돌렸다(같은 이벤트를 구독하는 principle.TendencyAnalyzedEventListener에서 실측됨).
    // handle()은 이 거부를 흡수하고 예외 없이 반환해야 한다.
    @Test
    void 실행기_제출이_거부돼도_예외가_전파되지_않는다() {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("simulated queue saturation");
        };
        NotificationService notificationService = new NotificationService(notificationRepository, new FakeNotificationSettingsRepository());
        TendencyAnalyzedEventListener rejectingListener = new TendencyAnalyzedEventListener(notificationService, rejectingExecutor);

        rejectingListener.handle(new TendencyAnalyzedEvent(USER_ID, ANALYSIS_RUN_ID, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"))));
    }
}
