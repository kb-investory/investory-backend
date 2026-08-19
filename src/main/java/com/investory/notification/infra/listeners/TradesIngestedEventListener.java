package com.investory.notification.infra.listeners;

import com.investory.ledger.domain.events.TradesIngestedEvent;
import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.services.NotificationService;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// ledger.domain.events를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 Command로 변환해
// domain/services에 넘긴다(CLAUDE.md §5). NotificationService는 TradesIngestedEvent의 존재를 모른다.
//
// @Async: 전용 Executor는 global/config/AsyncConfig에 아직 추가하지 않았다 — 지금은 @EnableAsync가
// 등록하는 기본 SimpleAsyncTaskExecutor로 동작한다. 알림 생성은 단순 DB insert 하나뿐이라 당장은
// 무리 없지만, 부하가 늘면 전용 풀 추가를 검토할 것.
@Component
public class TradesIngestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradesIngestedEventListener.class);

    private final NotificationService notificationService;

    public TradesIngestedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 비동기 리스너라 예외가 호출자(동기화 흐름)에 전파되지 않으므로 여기서 잡아 로그만 남긴다 —
    // 알림 생성 실패가 거래 적재 자체를 실패시키면 안 된다.
    @Async
    @EventListener
    public void handle(TradesIngestedEvent event) {
        try {
            notificationService.createIfEnabled(new CreateNotificationCommand(
                    event.userId(),
                    NotificationType.TRADE_INGESTED,
                    "새 거래내역이 들어왔어요",
                    event.insertedTradeCount() + "건의 거래가 새로 추가됐어요.",
                    event.accountId()));
        } catch (RuntimeException e) {
            log.error("거래 적재 알림 생성 실패. accountId={}", event.accountId(), e);
        }
    }
}
