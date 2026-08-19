package com.investory.market.domain.services;

import com.investory.market.domain.exception.MarketErrorCode;
import com.investory.market.domain.exception.MarketException;
import com.investory.market.domain.model.Security;
import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.repositories.SecurityPriceRepository;
import com.investory.market.domain.repositories.SecurityRepository;
import com.investory.market.domain.services.dto.query.GetSecurityPriceQuery;
import com.investory.market.domain.services.dto.query.SecuritySearchQuery;
import com.investory.market.domain.services.dto.result.SecurityDetailResult;
import com.investory.market.domain.services.dto.result.SecuritySearchResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 프론트 요청에 대해 "이미 DB에 저장돼 있는 값"을 그대로 조회해서 돌려주는 서비스.
 * KIS를 직접 호출하지 않는다 (KIS 호출/저장은 MarketDataSyncService/스케줄러의 역할).
 */
@Service
public class MarketDataQueryService {

    private final SecurityRepository securityRepository;
    private final SecurityPriceRepository securityPriceRepository;

    public MarketDataQueryService(SecurityRepository securityRepository, SecurityPriceRepository securityPriceRepository) {
        this.securityRepository = securityRepository;
        this.securityPriceRepository = securityPriceRepository;
    }

    public Security getStock(String securityCode) {
        return securityRepository.findBySecurityCode(securityCode)
                .orElseThrow(() -> new MarketException(MarketErrorCode.STOCK_NOT_FOUND));
    }

    // 특정 종목의 특정 날짜 시세를 조회한다. 그 날짜에 배치가 아직 안 돌았거나 저장된 게 없으면 404.
    public SecurityPrice getStockPrice(GetSecurityPriceQuery query) {
        Security security = getStock(query.securityCode());

        return securityPriceRepository.findBySecurityIdAndPriceDate(security.getSecurityId(), query.date())
                .orElseThrow(() -> new MarketException(MarketErrorCode.STOCK_PRICE_NOT_FOUND));
    }

    // securityId(내부 숫자 ID) 기준으로 종목 정보 + 가장 최근 시세를 함께 조회한다.
    // 종목이 없으면 404. 종목은 있지만 시세가 아직 한 번도 저장되지 않았다면 latestPrice는 null로 내려간다.
    public SecurityDetailResult getSecurityDetail(Long securityId) {
        Security security = securityRepository.findBySecurityId(securityId)
                .orElseThrow(() -> new MarketException(MarketErrorCode.STOCK_NOT_FOUND));

        SecurityPrice latestPrice = securityPriceRepository.findLatestBySecurityId(security.getSecurityId())
                .orElse(null);

        return new SecurityDetailResult(security, latestPrice);
    }

    // securityId(내부 숫자 ID) 기준 from~to(포함) 기간의 일별 시세 목록. 종목 존재 여부는 검증하지 않는다
    // (호출측이 이미 securityId를 알고 있는 상태에서 쓰는 용도라 굳이 재조회하지 않음). 데이터 없으면 빈 리스트.
    public List<SecurityPrice> getStockPrices(Long securityId, LocalDate from, LocalDate to) {
        return securityPriceRepository.findBySecurityIdAndDateRange(securityId, from, to);
    }

    // keyword/marketType으로 종목을 검색해 페이지 단위로 돌려준다.
    public SecuritySearchResult searchSecurities(SecuritySearchQuery query) {
        int offset = query.page() * query.size();

        List<Security> securities = securityRepository.search(query.keyword(), query.marketType(), offset, query.size());
        long totalElements = securityRepository.countSearch(query.keyword(), query.marketType());
        int totalPages = query.size() > 0 ? (int) Math.ceil((double) totalElements / query.size()) : 0;

        return new SecuritySearchResult(securities, query.page(), query.size(), totalElements, totalPages);
    }
}
