package com.investory.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
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
}
