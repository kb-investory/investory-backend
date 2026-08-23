package com.investory.broker.infra.schedulers;

import com.investory.broker.domain.services.BrokerProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BrokerProviderSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BrokerProviderSyncScheduler.class);

    private final BrokerProviderService brokerProviderService;

    public BrokerProviderSyncScheduler(BrokerProviderService brokerProviderService) {
        this.brokerProviderService = brokerProviderService;
    }

    // 정각 정렬 없이 앱 기동 시점부터 10분 간격으로 목 서버 기관(org) 목록을 broker_providers에 반영한다.
    // fixedDelay라 이전 실행이 끝난 시점 기준으로 10분 뒤 다음 실행이 잡힌다 — 외부 HTTP 호출이라
    // 간헐적으로 느려져도 실행이 겹치지 않는다.
    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void syncProviders() {
        try {
            brokerProviderService.syncProviders();
        } catch (RuntimeException e) {
            // 예외를 그대로 흘리면 Spring의 범용 스케줄링 에러 핸들러가 컨텍스트 없는 로그만 남긴다 —
            // 여기서 잡아 다음 회차(10분 뒤)로 넘어가되 원인을 남긴다.
            log.error("증권사 기관 목록 동기화 중 오류가 발생했습니다.", e);
        }
    }
}
