package com.investory.market.presentation.controller;

import com.investory.market.domain.exception.MarketErrorCode;
import com.investory.market.domain.exception.MarketException;
import com.investory.market.domain.services.MarketDataQueryService;
import com.investory.market.domain.services.dto.query.SecuritySearchQuery;
import com.investory.market.domain.services.dto.result.SecurityDetailResult;
import com.investory.market.domain.services.dto.result.SecuritySearchResult;
import com.investory.market.presentation.dto.response.SecurityDetailResponse;
import com.investory.market.presentation.dto.response.SecurityListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/market/securities")
public class SecurityController {

    private final MarketDataQueryService marketDataQueryService;

    public SecurityController(MarketDataQueryService marketDataQueryService) {
        this.marketDataQueryService = marketDataQueryService;
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
        SecurityDetailResult result = marketDataQueryService.getSecurityDetail(Long.valueOf(String.valueOf(securityId)));
        return ResponseEntity.ok(SecurityDetailResponse.from(result));
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
