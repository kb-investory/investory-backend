package com.investory.broker.infra.schedulers;

import com.investory.broker.domain.repositories.ActiveConnectionSyncTarget;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.services.BrokerConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// 연결된 계좌를 주기적으로 재동기화한다(CLAUDE.md §11). 계좌 수 규모가 아직 정해지지 않아 지금은
// 활성 연결 전체를 순차로 순회하는 단순한 형태로 시작한다 — 나중에 계좌 수가 늘어 한 배치 실행
// 시간이 다음 주기를 넘기면, 조회에 페이지네이션을 붙이고 결과를 전용 스레드풀에 넘기는 방식으로
// 확장한다(§3-② fetch/write 분리는 이미 BrokerAccountSyncService가 담당).
@Component
public class BrokerAccountSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BrokerAccountSyncScheduler.class);

    private final BrokerConnectionRepository brokerConnectionRepository;
    private final BrokerConnectionService brokerConnectionService;

    public BrokerAccountSyncScheduler(BrokerConnectionRepository brokerConnectionRepository,
                                       BrokerConnectionService brokerConnectionService) {
        this.brokerConnectionRepository = brokerConnectionRepository;
        this.brokerConnectionService = brokerConnectionService;
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
        for (ActiveConnectionSyncTarget target : targets) {
            try {
                brokerConnectionService.syncForBatch(
                        target.userId(), target.connectionId(), target.mockProfileCode(), target.brokerCode());
            } catch (RuntimeException e) {
                // 연결 하나의 실패가 나머지 연결의 동기화를 막으면 안 된다. syncForBatch 내부는 외부
                // 호출 실패를 이미 잡아 account_sync_batches에 FAILED로 기록하므로, 여기서 잡는 예외는
                // 그 앞뒤(배치 레코드 생성/조회 등) 인프라 예외뿐이다 — 순회가 끊기지 않도록 로그만 남긴다.
                log.error("계좌 동기화 배치 중 오류가 발생했습니다. connectionId={}", target.connectionId(), e);
            }
        }
        // 배치서버 분리 전/후 성능 비교용. SYNC_BATCH 접두어로 grep해서 elapsedMs를 뽑아 비교한다.
        log.info("SYNC_BATCH targetCount={} elapsedMs={}", targets.size(), System.currentTimeMillis() - start);
    }
}
