package com.investory.market.infra.scheduler;

import com.investory.market.domain.services.MarketDataSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataScheduler {

    private final MarketDataSyncService marketDataSyncService;

    public MarketDataScheduler(MarketDataSyncService marketDataSyncService) {
        this.marketDataSyncService = marketDataSyncService;
    }

    // 매일(평일) KST 22:00에 이미 등록된 종목들의 마스터 정보 + 그날치 시세를 갱신한다.
    // cron: 초 분 시 일 월 요일. 서버가 UTC로 동작하므로 zone을 명시해 KST 기준으로 고정한다.
    // 개발시 주석 해제 후 사용
     @Scheduled(cron = "0 0 22 * * MON-FRI", zone = "Asia/Seoul")
    public void syncDailyMarketData() {
        marketDataSyncService.syncAllTrackedStocks();
    }
}
