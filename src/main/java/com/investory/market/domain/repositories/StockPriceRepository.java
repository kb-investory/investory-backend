package com.investory.market.domain.repositories;

import com.investory.market.domain.model.StockPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository {
    Optional<StockPrice> findBySecurityIdAndPriceDate(Long securityId, LocalDate priceDate);

    // 해당 종목의 가장 최근 날짜 시세 1건 (없으면 empty)
    Optional<StockPrice> findLatestBySecurityId(Long securityId);

    // from~to(포함) 기간의 일별 시세 목록, price_date 오름차순
    List<StockPrice> findBySecurityIdAndDateRange(Long securityId, LocalDate from, LocalDate to);

    StockPrice save(StockPrice stockPrice);
}
