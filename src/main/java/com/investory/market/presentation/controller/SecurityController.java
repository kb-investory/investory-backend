package com.investory.market.presentation.controller;

import com.investory.market.domain.exception.MarketErrorCode;
import com.investory.market.domain.exception.MarketException;
import com.investory.market.domain.model.Security;
import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.services.MarketDataQueryService;
import com.investory.market.domain.services.MarketDataSyncService;
import com.investory.market.domain.services.dto.command.SyncSecurityCommand;
import com.investory.market.domain.services.dto.query.GetSecurityPriceQuery;
import com.investory.market.domain.services.dto.query.SecuritySearchQuery;
import com.investory.market.domain.services.dto.result.SecurityDetailResult;
import com.investory.market.domain.services.dto.result.SecuritySearchResult;
import com.investory.market.presentation.dto.response.SecurityDetailResponse;
import com.investory.market.presentation.dto.response.SecurityListResponse;
import com.investory.market.presentation.dto.response.SecurityPriceResponse;
import com.investory.market.presentation.dto.response.SecurityResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/market/securities")
public class SecurityController {

    private final MarketDataQueryService marketDataQueryService;
    private final MarketDataSyncService marketDataSyncService;

    public SecurityController(MarketDataQueryService marketDataQueryService,
                               MarketDataSyncService marketDataSyncService) {
        this.marketDataQueryService = marketDataQueryService;
        this.marketDataSyncService = marketDataSyncService;
    }

    // 종목 목록 조회 (검색/시장구분 필터 + 페이지네이션)
    // 예: GET /market/securities?keyword=삼성&marketType=KOSPI&page=0&size=20
    @GetMapping
    public ResponseEntity<SecurityListResponse> getSecurities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String marketType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        SecuritySearchQuery query = toSearchQuery(keyword, marketType, page, size);
        SecuritySearchResult result = marketDataQueryService.searchSecurities(query);
        return ResponseEntity.ok(SecurityListResponse.from(result));
    }

    // 종목 단건 조회 (securityId 기준) + 가장 최근 시세
    // 예: GET /market/securities/101
    @GetMapping("/{securityId}")
    public ResponseEntity<SecurityDetailResponse> getSecurity(@PathVariable Long securityId) {
        SecurityDetailResult result = marketDataQueryService.getSecurityDetail(securityId);
        return ResponseEntity.ok(SecurityDetailResponse.from(result));
    }

    // ===== 종목코드(securityCode) 기준 조회/동기화 =====
    // securityId 기준 조회(/{securityId})와 경로 패턴이 겹쳐 /code 하위로 구분한다.
    // DB에 이미 저장된 값을 그대로 돌려준다. KIS를 다시 호출하지 않는다.

    // 종목 마스터 정보 조회
    @GetMapping("/code/{securityCode}")
    public ResponseEntity<SecurityResponse> getStock(@PathVariable String securityCode) {
        Security security = marketDataQueryService.getStock(securityCode);
        return ResponseEntity.ok(SecurityResponse.from(security));
    }

    // 특정 날짜의 시세 조회. date를 안 주면 오늘 날짜로 조회한다. (예: /market/securities/code/000660/prices?date=2026-08-03)
    @GetMapping("/code/{securityCode}/prices")
    public ResponseEntity<SecurityPriceResponse> getStockPrice(
            @PathVariable String securityCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        SecurityPrice securityPrice = marketDataQueryService.getStockPrice(new GetSecurityPriceQuery(securityCode, targetDate));
        return ResponseEntity.ok(SecurityPriceResponse.from(securityPrice));
    }

    // ===== 동기화용 (수동 트리거/테스트용. 실제 운영에서는 MarketDataScheduler가 매일 17시에 대신 호출) =====

    // KIS에서 종목 마스터 정보만 가져와 securities 테이블에 upsert한다.
    @PostMapping("/code/{securityCode}/sync-info")
    public ResponseEntity<SecurityResponse> syncInfo(@PathVariable String securityCode) {
        Security security = marketDataSyncService.syncStockInfo(new SyncSecurityCommand(securityCode));
        return ResponseEntity.ok(SecurityResponse.from(security));
    }

    // KIS에서 오늘자 시세만 가져와 security_daily_prices 테이블에 저장한다. (securities에 없으면 먼저 채우고 진행)
    @PostMapping("/code/{securityCode}/sync-price")
    public ResponseEntity<SecurityPriceResponse> syncPrice(@PathVariable String securityCode) {
        SecurityPrice securityPrice = marketDataSyncService.syncDailyPrice(new SyncSecurityCommand(securityCode));
        return ResponseEntity.ok(SecurityPriceResponse.from(securityPrice));
    }

    // securities + security_daily_prices 두 테이블을 한 번에 채운다. (신규 종목을 추적 대상에 등록할 때 사용 - 이후엔 매일 배치가 자동 갱신)
    @PostMapping("/code/{securityCode}/sync")
    public ResponseEntity<Void> sync(@PathVariable String securityCode) {
        marketDataSyncService.syncStockAndPrice(new SyncSecurityCommand(securityCode));
        return ResponseEntity.ok().build();
    }

    // marketType 문자열을 검증하며 enum으로 변환한다. 잘못된 값이면 400(INVALID_MARKET_TYPE)으로 응답한다.
    private SecuritySearchQuery toSearchQuery(String keyword, String marketType, Integer page, Integer size) {
        try {
            return SecuritySearchQuery.of(keyword, marketType, page, size);
        } catch (IllegalArgumentException e) {
            throw new MarketException(MarketErrorCode.INVALID_MARKET_TYPE);
        }
    }
}
