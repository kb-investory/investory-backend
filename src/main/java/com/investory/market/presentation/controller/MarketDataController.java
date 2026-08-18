package com.investory.market.presentation.controller;

import com.investory.market.domain.model.Security;
import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.services.MarketDataQueryService;
import com.investory.market.domain.services.MarketDataSyncService;
import com.investory.market.domain.services.dto.command.SyncSecurityCommand;
import com.investory.market.domain.services.dto.query.GetSecurityPriceQuery;
import com.investory.market.presentation.dto.response.SecurityPriceResponse;
import com.investory.market.presentation.dto.response.SecurityResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/markets/securities")
public class MarketDataController {

    private final MarketDataSyncService marketDataSyncService;
    private final MarketDataQueryService marketDataQueryService;

    public MarketDataController(MarketDataSyncService marketDataSyncService,
                                 MarketDataQueryService marketDataQueryService) {
        this.marketDataSyncService = marketDataSyncService;
        this.marketDataQueryService = marketDataQueryService;
    }

    // ===== 조회용 (프론트가 호출하는 쪽) =====
    // DB에 이미 저장돼 있는 값을 그대로 돌려준다. KIS를 다시 호출하지 않는다.

    // 종목 마스터 정보 조회
    @GetMapping("/{securityCode}")
    public ResponseEntity<SecurityResponse> getStock(@PathVariable String securityCode) {
        Security security = marketDataQueryService.getStock(securityCode);
        return ResponseEntity.ok(SecurityResponse.from(security));
    }

    // 특정 날짜의 시세 조회. date를 안 주면 오늘 날짜로 조회한다. (예: /market/stocks/000660/prices?date=2026-08-03)
    @GetMapping("/{securityCode}/prices")
    public ResponseEntity<SecurityPriceResponse> getStockPrice(
            @PathVariable String securityCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        SecurityPrice securityPrice = marketDataQueryService.getStockPrice(new GetSecurityPriceQuery(securityCode, targetDate));
        return ResponseEntity.ok(SecurityPriceResponse.from(securityPrice));
    }

    // ===== 동기화용 (수동 트리거/테스트용. 실제 운영에서는 MarketDataScheduler가 매일 17시에 대신 호출) =====

    // KIS에서 종목 마스터 정보만 가져와 securities 테이블에 upsert한다.
    @PostMapping("/{securityCode}/sync-info")
    public ResponseEntity<SecurityResponse> syncInfo(@PathVariable String securityCode) {
        Security security = marketDataSyncService.syncStockInfo(new SyncSecurityCommand(securityCode));
        return ResponseEntity.ok(SecurityResponse.from(security));
    }

    // KIS에서 오늘자 시세만 가져와 security_daily_prices 테이블에 저장한다. (securities에 없으면 먼저 채우고 진행)
    @PostMapping("/{securityCode}/sync-price")
    public ResponseEntity<SecurityPriceResponse> syncPrice(@PathVariable String securityCode) {
        SecurityPrice securityPrice = marketDataSyncService.syncDailyPrice(new SyncSecurityCommand(securityCode));
        return ResponseEntity.ok(SecurityPriceResponse.from(securityPrice));
    }

    // securities + security_daily_prices 두 테이블을 한 번에 채운다. (신규 종목을 추적 대상에 등록할 때 사용 - 이후엔 매일 배치가 자동 갱신)
    @PostMapping("/{securityCode}/sync")
    public ResponseEntity<Void> sync(@PathVariable String securityCode) {
        marketDataSyncService.syncStockAndPrice(new SyncSecurityCommand(securityCode));
        return ResponseEntity.ok().build();
    }
}
