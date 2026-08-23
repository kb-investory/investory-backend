package com.investory.notification.infra.listeners;

import com.investory.ledger.domain.events.TradesIngestedEvent;
import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.services.NotificationService;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// ledger.domain.events를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 Command로 변환해
// domain/services에 넘긴다(CLAUDE.md §5). NotificationService는 TradesIngestedEvent의 존재를 모른다.
//
// @TransactionalEventListener(AFTER_COMMIT): TradeIngestionService.ingestTrades()는 @Transactional
// 안에서 publishEvent()를 호출한다. 예전엔 @EventListener라 publishEvent() 호출 즉시(=메서드
// 리턴 전, 커밋 전) 리스너가 실행돼, flush 시점에 트랜잭션이 롤백돼도 이미 나간 알림은 취소되지
// 않았다(#193). AFTER_COMMIT으로 등록해 실제 커밋 이후에만 실행되게 한다.
//
// @Async("notificationExecutor"): SimpleAsyncTaskExecutor(무제한 스레드 생성)로 동작하던 걸
// global/config/AsyncConfig의 bounded 풀로 옮겼다(#194).
@Component
public class TradesIngestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradesIngestedEventListener.class);

    private final NotificationService notificationService;

    public TradesIngestedEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 비동기 리스너라 예외가 호출자(동기화 흐름)에 전파되지 않으므로 여기서 잡아 로그만 남긴다 —
    // 알림 생성 실패가 거래 적재 자체를 실패시키면 안 된다.
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
