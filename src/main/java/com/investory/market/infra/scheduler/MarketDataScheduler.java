package com.investory.market.infra.scheduler;

import com.investory.market.domain.services.MarketDataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataScheduler.class);

    private final MarketDataSyncService marketDataSyncService;

    public MarketDataScheduler(MarketDataSyncService marketDataSyncService) {
        this.marketDataSyncService = marketDataSyncService;
    }

    // 매일(평일) KST 22:00에 이미 등록된 종목들의 마스터 정보 + 그날치 시세를 갱신한다.
    // cron: 초 분 시 일 월 요일. 서버가 UTC로 동작하므로 zone을 명시해 KST 기준으로 고정한다.
    // 개발시 주석 해제 후 사용
     @Scheduled(cron = "0 0 22 * * MON-FRI", zone = "Asia/Seoul")
    public void syncDailyMarketData() {
        try {
            marketDataSyncService.syncAllTrackedStocks();
        } catch (RuntimeException e) {
            // 하루 1번뿐인 배치라 여기서 조용히 실패하면 다음날 22시까지 아무도 모른 채 시세가
            // 통째로 빠진다 — 보유평가금액/성향분석 변동성 등 시세에 의존하는 모든 계산에 전파되므로
            // 반드시 원인을 남긴다.
            log.error("일별 시세 동기화 중 오류가 발생했습니다.", e);
        }
    }
}
