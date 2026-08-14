package com.investory.broker.infra.schedulers;

import com.investory.broker.domain.services.BrokerProviderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BrokerProviderSyncScheduler {

    private final BrokerProviderService brokerProviderService;

    public BrokerProviderSyncScheduler(BrokerProviderService brokerProviderService) {
        this.brokerProviderService = brokerProviderService;
    }

    // 정각 정렬 없이 앱 기동 시점부터 10분 간격으로 목 서버 기관(org) 목록을 broker_providers에 반영한다.
    // fixedDelay라 이전 실행이 끝난 시점 기준으로 10분 뒤 다음 실행이 잡힌다 — 외부 HTTP 호출이라
    // 간헐적으로 느려져도 실행이 겹치지 않는다.
    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void syncProviders() {
        brokerProviderService.syncProviders();
    }
}
