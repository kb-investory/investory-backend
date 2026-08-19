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
}
