package com.investory.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // tendency의 원칙 이행 성향(6번) 분석이 원칙 항목마다 LLM을 호출하는데, 순차 호출이라 항목 수만큼
    // 지연이 그대로 누적돼 프론트 타임아웃을 유발했다. 이 풀로 항목별 LLM 호출(분류/채점)을 병렬 실행한다.
    // 사용자당 원칙 개수가 많지 않고(보통 한 자릿수) LLM API 자체의 동시 호출 한도도 있어 풀을 작게 둔다.
    @Bean
    public Executor tendencyLlmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("tendency-llm-");
        executor.initialize();
        return executor;
    }

    // principle.infra.listeners.TendencyAnalyzedEventListener 전용. Spring의 이벤트 발행은 기본이
    // 동기라, 이 리스너가 LLM로 추천을 생성하는 동안 tendency.AnalysisRunService.runAnalysis()가
    // 그 시간만큼 그대로 블로킹됐다 — 분석 자체(및 DB 커밋)는 이미 끝났는데 응답만 못 나가는 상태.
    // POST /tendency/analyses 504의 실제 원인이었다. @Async로 이 리스너만 떼어내 응답 경로에서 완전히 분리한다.
    @Bean
    public Executor principleRecommendationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("principle-reco-");
        executor.initialize();
        return executor;
    }

    // JournalService.labelTradeNotes() 전용. 일지 저장/수정(POST·PUT journal)이 사용자 응답을 기다리는
    // 요청 경로인데, 근거 라벨링 LLM 호출이 거래 노트 수만큼 순차 실행돼 노트가 많은 날일수록 응답이
    // 그만큼 느려졌다(#196). 이 풀로 노트별 classify() 호출을 병렬 실행한다. 한 일지에 달리는 노트 수는
    // 보통 한 자릿수라 tendencyLlmExecutor와 비슷하게 작게 둔다.
    @Bean
    public Executor journalLabelingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("journal-labeling-");
        executor.initialize();
        return executor;
    }

    // PrincipleService.refreshRecommendationsForRun() 안에서 항목별 추천 생성 LLM 호출을 병렬로
    // 돌리는 전용 풀. principleRecommendationExecutor(리스너를 응답 경로에서 떼어내는 바깥쪽 풀)와
    // 반드시 분리한다 — 같은 풀을 재사용하면 리스너가 그 풀의 스레드 하나를 잡은 채 안에서 또 그
    // 풀에 작업을 제출하고 join()으로 기다리게 되어, 풀이 가득 찼을 때 자기 자신을 기다리며 멈추는
    // 자기잠금(self-deadlock)이 날 수 있다.
    @Bean
    public Executor recommendationGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("principle-reco-gen-");
        executor.initialize();
        return executor;
    }

    // notification.infra.listeners.TradesIngestedEventListener, notification.TendencyAnalyzedEventListener
    // 전용(#194). 둘 다 지금까지 전용 Executor가 없어 @EnableAsync 기본값인 SimpleAsyncTaskExecutor로
    // 동작했는데, 이 Executor는 풀링/큐잉이 없이 호출마다 스레드를 무제한 생성한다 — 거래 적재나
    // 성향분석 완료가 몰리면 스레드가 걷잡을 수 없이 늘어날 수 있다. 알림 생성은 DB insert 하나뿐이라
    // 풀을 작게 둔다.
    @Bean
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }

    // BrokerAccountSyncScheduler.syncAllActiveConnections() 전용(#195). 활성 연결 전체를 단일
    // 스레드로 순차 처리하면 배치 소요 시간이 연결 수에 비례해 선형으로 늘어나 결국 스케줄 주기(5분)를
    // 넘긴다. 계좌별 syncForBatch() 호출을 이 풀에 병렬 제출한다. write 단계(REQUIRES_NEW 트랜잭션)만
    // DB 커넥션을 쥐므로 풀 크기는 HikariCP 기본 풀 크기(10, DatabaseConfig에 명시값 없음)보다 다소
    // 여유 있게 잡아도 된다 — fetch(외부 HTTP) 단계는 커넥션 없이 대기하기 때문. maxPoolSize(20)가
    // Hikari 풀보다 큰 상태에서 write 단계가 동시에 몰리면 초과분은 HikariCP의 기본
    // connectionTimeout(30s) 동안 커넥션을 기다리다 처리되므로 즉시 실패하지는 않는다 — 다만 이
    // 대기 자체가 지연으로 체감될 수 있으니, 실제 배치 소요 시간을 관찰하고도 병목이면 HikariCP
    // 풀 크기를 키우는 걸 다음으로 검토한다(DB 부하에 영향을 주는 별개 판단이라 이번엔 건드리지 않았다).
    // AnalysisRunService.runAnalysis()/executeAnalysis() 전용(#207). POST /tendency/analyses가
    // 요청 스레드에서 6개 분석 항목을 전부 동기로 처리하느라 9~13초 걸리던 걸, 요청 스레드는 REQUESTED
    // 상태 행만 만들고 즉시 202를 반환하도록 바꾸면서 실제 분석(executeAnalysis)을 이 풀로 옮겼다.
    // tendencyLlmExecutor와 반드시 분리한다 — executeAnalysis()가 PrincipleAdherenceAnalysisService를
    // 거쳐 tendencyLlmExecutor에 하위 작업을 제출하고 join()으로 기다리는데, 같은 풀을 쓰면 외부
    // 작업이 그 풀의 스레드 하나를 잡은 채 같은 풀에 하위 작업을 또 제출해 기다리는 자기잠금이 날 수
    // 있다(recommendationGenerationExecutor와 같은 이유). 아래 풀 크기는 시작값이다 — 이 전환의
    // 핵심은 클라이언트 동시 요청 수와 실제 처리 동시성을 분리하는 것이므로, 실제 값은 loadtest로
    // 재측정해서 정한다(#207 후속).
    @Bean
    public Executor analysisRunExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analysis-run-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor brokerSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("broker-sync-");
        executor.initialize();
        return executor;
    }
}
