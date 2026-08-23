package com.investory.notification.infra.listeners;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.services.NotificationService;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

// tendency.domain.events를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 Command로 변환해
// domain/services에 넘긴다(CLAUDE.md §5). NotificationService는 TendencyAnalyzedEvent의 존재를 모른다.
//
// 실행기에 직접 제출: 예전엔 @Async("notificationExecutor")로 선언적으로 떼어냈는데, 같은 이벤트를
// 구독하는 principle.TendencyAnalyzedEventListener가 1000 VU 부하테스트에서 정확히 이 패턴 때문에
// POST /tendency/analyses를 500으로 되돌린 게 확인됐다 — executor 큐가 가득 차면 "제출 자체"가
// 거부되는 TaskRejectedException이 handle() 본문(과 그 안의 try/catch)을 거치지 않고 Spring AOP
// 프록시 단계에서 곧장 던져져, 동기 발행 경로(publishEvent())를 타고 그대로 호출자까지 전파된다.
// 같은 publishEvent() 호출이 이 리스너도 함께 구독하므로 동일한 결함을 그대로 안고 있어 같이
// 고친다. handle() 자체는 동기(@EventListener만)로 두고, 실행기 제출을 여기서 직접 try/catch로
// 감싼다 — journalLabelingExecutor/brokerSyncExecutor/tendencyLlmExecutor 호출부와 같은 패턴.
//
// 빈 이름을 명시한다 — principle도 같은 이벤트를 구독하는 동명(TendencyAnalyzedEventListener) 클래스를
// 갖고 있어(xxxEventListener 명명 규칙상 자연스러운 충돌), 둘 다 default bean name을 쓰면
// ConflictingBeanDefinitionException으로 컨텍스트 초기화가 실패한다.
@Component("notificationTendencyAnalyzedEventListener")
public class TendencyAnalyzedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TendencyAnalyzedEventListener.class);

    private final NotificationService notificationService;
    private final Executor notificationExecutor;

    public TendencyAnalyzedEventListener(NotificationService notificationService,
                                          @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    @EventListener
    public void handle(TendencyAnalyzedEvent event) {
        try {
            CompletableFuture.runAsync(() -> createNotification(event), notificationExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("성향분석 완료 알림 작업 제출 실패(큐 포화) — 이번 알림 생성은 건너뜁니다. analysisRunId={}",
                    event.analysisRunId(), e);
        }
    }

    // 알림 생성 실패가 성향분석 자체를 실패시키면 안 되므로 여기서 잡아 로그만 남긴다.
    private void createNotification(TendencyAnalyzedEvent event) {
        try {
            notificationService.createIfEnabled(new CreateNotificationCommand(
                    event.userId(),
                    NotificationType.TENDENCY_ANALYZED,
                    "투자성향 분석이 완료됐어요",
                    "성향 분석 결과를 확인해보세요.",
                    event.analysisRunId()));
        } catch (RuntimeException e) {
            log.error("성향분석 완료 알림 생성 실패. analysisRunId={}", event.analysisRunId(), e);
        }
    }
}
