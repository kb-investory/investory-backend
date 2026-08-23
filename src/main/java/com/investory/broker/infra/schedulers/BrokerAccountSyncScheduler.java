package com.investory.broker.infra.schedulers;

import com.investory.broker.domain.repositories.ActiveConnectionSyncTarget;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.services.BrokerConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

// 연결된 계좌를 주기적으로 재동기화한다(CLAUDE.md §11). 예전엔 활성 연결 전체를 단일 스레드로
// 순차 순회해, 연결 수가 늘면 한 배치 실행 시간이 연결 수에 비례해 선형으로 늘어나 결국 스케줄
// 주기(5분)를 넘겼다(#195). 지금은 계좌별 동기화를 brokerSyncExecutor(global/config/AsyncConfig)에
// 병렬 제출한다(§3-② fetch/write 분리는 이미 BrokerAccountSyncService가 담당). 페이지네이션은
// 이번엔 건드리지 않았다 — 지금 규모에선 메모리보다 동시성 부재가 더 급하다.
@Component
public class BrokerAccountSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BrokerAccountSyncScheduler.class);

    private final BrokerConnectionRepository brokerConnectionRepository;
    private final BrokerConnectionService brokerConnectionService;
    private final Executor brokerSyncExecutor;

    public BrokerAccountSyncScheduler(BrokerConnectionRepository brokerConnectionRepository,
                                       BrokerConnectionService brokerConnectionService,
                                       @Qualifier("brokerSyncExecutor") Executor brokerSyncExecutor) {
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.brokerConnectionService = brokerConnectionService;
        this.brokerSyncExecutor = brokerSyncExecutor;
    }

    // fixedDelay라 이전 실행이 끝난 시점 기준으로 5분 뒤 다음 실행이 잡힌다 — 목 피드라 레이트리밋은
    // 없지만, 연결 수가 늘어도 이전 순회가 다음 순회와 겹치지 않는다.
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void syncAllActiveConnections() {
        long start = System.currentTimeMillis();
        List<ActiveConnectionSyncTarget> targets;
        try {
            targets = brokerConnectionRepository.findAllActiveForSync();
        } catch (RuntimeException e) {
            // 루프 안 계좌별 catch와 달리 이 조회 자체는 보호돼 있지 않았다 — 실패하면 이번 회차
            // 전체를 건너뛰고 다음 주기(5분 뒤)에 다시 시도한다.
            log.error("동기화 대상 연결 목록을 조회하는 중 오류가 발생했습니다.", e);
            return;
        }
        List<CompletableFuture<Void>> futures = targets.stream()
                .map(target -> CompletableFuture.runAsync(() -> syncOne(target), brokerSyncExecutor))
                .collect(Collectors.toList());
        futures.forEach(CompletableFuture::join);
        // 배치서버 분리 전/후 성능 비교용. SYNC_BATCH 접두어로 grep해서 elapsedMs를 뽑아 비교한다.
        log.info("SYNC_BATCH targetCount={} elapsedMs={}", targets.size(), System.currentTimeMillis() - start);
    }

    private void syncOne(ActiveConnectionSyncTarget target) {
        try {
            brokerConnectionService.syncForBatch(
                    target.userId(), target.connectionId(), target.mockProfileCode(), target.brokerCode());
        } catch (RuntimeException e) {
            // 연결 하나의 실패가 나머지 연결의 동기화를 막으면 안 된다. syncForBatch 내부는 외부
            // 호출 실패를 이미 잡아 account_sync_batches에 FAILED로 기록하므로, 여기서 잡는 예외는
            // 그 앞뒤(배치 레코드 생성/조회 등) 인프라 예외뿐이다 — 로그만 남기고 다른 연결의
            // CompletableFuture에 영향을 주지 않는다.
            log.error("계좌 동기화 배치 중 오류가 발생했습니다. connectionId={}", target.connectionId(), e);
        }
    }
}
