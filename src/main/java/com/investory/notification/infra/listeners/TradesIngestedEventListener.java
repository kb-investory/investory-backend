package com.investory.notification.infra.listeners;

import com.investory.ledger.domain.events.TradesIngestedEvent;
import com.investory.notification.domain.constant.NotificationType;
import com.investory.notification.domain.services.NotificationService;
import com.investory.notification.domain.services.dto.command.CreateNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

// ledger.domain.events를 참조하는 유일한 지점 — 받는 즉시 notification 자신의 Command로 변환해
// domain/services에 넘긴다(CLAUDE.md §5). NotificationService는 TradesIngestedEvent의 존재를 모른다.
//
// @TransactionalEventListener(AFTER_COMMIT): TradeIngestionService.ingestTrades()는 @Transactional
// 안에서 publishEvent()를 호출한다. 예전엔 @EventListener라 publishEvent() 호출 즉시(=메서드
// 리턴 전, 커밋 전) 리스너가 실행돼, flush 시점에 트랜잭션이 롤백돼도 이미 나간 알림은 취소되지
// 않았다(#193). AFTER_COMMIT으로 등록해 실제 커밋 이후에만 실행되게 한다.
//
// 실행기에 직접 제출: 예전엔 @Async("notificationExecutor")로 선언적으로 떼어냈는데(#194,
// SimpleAsyncTaskExecutor의 무제한 스레드 생성을 막기 위함이었다), tendency 쪽의 동일 패턴이
// 1000 VU 부하테스트에서 executor 큐 포화 시 "제출 자체"가 거부되는 TaskRejectedException이
// handle() 본문(과 그 안의 try/catch)을 거치지 않고 Spring AOP 프록시 단계에서 곧장 던져지는 걸로
// 확인됐다 — AFTER_COMMIT 콜백 스택을 타고 그대로 동기화 호출자까지 전파될 수 있는 동일한 구조라
// 같이 고친다. handle() 자체는 트랜잭션 커밋 이후 동기 콜백으로만 두고(@TransactionalEventListener는
// 유지), 실행기 제출을 여기서 직접 try/catch로 감싼다.
@Component
public class TradesIngestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradesIngestedEventListener.class);

    private final NotificationService notificationService;
    private final Executor notificationExecutor;

    public TradesIngestedEventListener(NotificationService notificationService,
                                        @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TradesIngestedEvent event) {
        try {
            CompletableFuture.runAsync(() -> createNotification(event), notificationExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("거래 적재 알림 작업 제출 실패(큐 포화) — 이번 알림 생성은 건너뜁니다. accountId={}",
                    event.accountId(), e);
        }
    }

    // 알림 생성 실패가 거래 적재 자체를 실패시키면 안 되므로 여기서 잡아 로그만 남긴다.
    private void createNotification(TradesIngestedEvent event) {
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
