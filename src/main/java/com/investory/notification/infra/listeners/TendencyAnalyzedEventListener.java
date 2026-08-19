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
// @Async: 전용 Executor는 global/config/AsyncConfig에 아직 추가하지 않았다(공유 인프라라 팀 확인 후
// 별도로 추가하기로 함) — 지금은 @EnableAsync가 등록하는 기본 SimpleAsyncTaskExecutor로 동작한다.
// 알림 생성은 LLM 호출 없이 단순 DB insert 하나뿐이라 당장은 무리 없지만, 부하가 늘면 전용 풀
// 추가를 검토할 것.
@Component
public class TendencyAnalyzedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TendencyAnalyzedEventListener.class);

    private final NotificationService notificationService;

    public TendencyAnalyzedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 비동기 리스너라 예외가 호출자(분석 실행 흐름)에 전파되지 않으므로 여기서 잡아 로그만 남긴다 —
    // 알림 생성 실패가 성향분석 자체를 실패시키면 안 된다.
    @Async
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
