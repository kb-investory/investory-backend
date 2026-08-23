package com.investory.notification.infra.listeners;

import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.services.NotificationService;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// tendency.domain.events를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 Command로 변환해
// domain/services에 넘긴다(CLAUDE.md §5). NotificationService는 TendencyAnalyzedEvent의 존재를 모른다.
//
// @Async("notificationExecutor"): SimpleAsyncTaskExecutor(무제한 스레드 생성)로 동작하던 걸
// global/config/AsyncConfig의 bounded 풀로 옮겼다(#194).
//
// 빈 이름을 명시한다 — principle도 같은 이벤트를 구독하는 동명(TendencyAnalyzedEventListener) 클래스를
// 갖고 있어(xxxEventListener 명명 규칙상 자연스러운 충돌), 둘 다 default bean name을 쓰면
// ConflictingBeanDefinitionException으로 컨텍스트 초기화가 실패한다.
@Component("notificationTendencyAnalyzedEventListener")
public class TendencyAnalyzedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TendencyAnalyzedEventListener.class);

    private final NotificationService notificationService;

    public TendencyAnalyzedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 비동기 리스너라 예외가 호출자(분석 실행 흐름)에 전파되지 않으므로 여기서 잡아 로그만 남긴다 —
    // 알림 생성 실패가 성향분석 자체를 실패시키면 안 된다.
    @Async("notificationExecutor")
    @EventListener
    public void handle(TendencyAnalyzedEvent event) {
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
