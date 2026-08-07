package com.investory.market.presentation.controller;

import com.investory.market.domain.model.Stock;
import com.investory.market.domain.model.StockPrice;
import com.investory.market.domain.services.MarketDataQueryService;
import com.investory.market.domain.services.MarketDataSyncService;
import com.investory.market.domain.services.dto.command.SyncStockCommand;
import com.investory.market.domain.services.dto.query.GetStockPriceQuery;
import com.investory.market.presentation.dto.response.StockPriceResponse;
import com.investory.market.presentation.dto.response.StockResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/market/securities")
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
    @GetMapping("/{stockCode}")
    public ResponseEntity<StockResponse> getStock(@PathVariable String stockCode) {
        Stock stock = marketDataQueryService.getStock(stockCode);
        return ResponseEntity.ok(StockResponse.from(stock));
    }

    // 특정 날짜의 시세 조회. date를 안 주면 오늘 날짜로 조회한다. (예: /market/stocks/000660/prices?date=2026-08-03)
    @GetMapping("/{stockCode}/prices")
    public ResponseEntity<StockPriceResponse> getStockPrice(
            @PathVariable String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        StockPrice stockPrice = marketDataQueryService.getStockPrice(new GetStockPriceQuery(stockCode, targetDate));
        return ResponseEntity.ok(StockPriceResponse.from(stockPrice));
    }

    // ===== 동기화용 (수동 트리거/테스트용. 실제 운영에서는 MarketDataScheduler가 매일 17시에 대신 호출) =====

    // KIS에서 종목 마스터 정보만 가져와 stocks 테이블에 upsert한다.
    @PostMapping("/{stockCode}/sync-info")
    public ResponseEntity<StockResponse> syncInfo(@PathVariable String stockCode) {
        Stock stock = marketDataSyncService.syncStockInfo(new SyncStockCommand(stockCode));
        return ResponseEntity.ok(StockResponse.from(stock));
    }

    // KIS에서 오늘자 시세만 가져와 stock_prices 테이블에 저장한다. (stocks에 없으면 먼저 채우고 진행)
    @PostMapping("/{stockCode}/sync-price")
    public ResponseEntity<StockPriceResponse> syncPrice(@PathVariable String stockCode) {
        StockPrice stockPrice = marketDataSyncService.syncDailyPrice(new SyncStockCommand(stockCode));
        return ResponseEntity.ok(StockPriceResponse.from(stockPrice));
    }

    // stocks + stock_prices 두 테이블을 한 번에 채운다. (신규 종목을 추적 대상에 등록할 때 사용 - 이후엔 매일 배치가 자동 갱신)
    @PostMapping("/{stockCode}/sync")
    public ResponseEntity<Void> sync(@PathVariable String stockCode) {
        marketDataSyncService.syncStockAndPrice(new SyncStockCommand(stockCode));
        return ResponseEntity.ok().build();
    }
}
