package com.investory.market.domain.repositories;

import com.kbinvestory.backend.market.domain.model.StockPrice;

import java.time.LocalDate;
import java.util.Optional;

public interface StockPriceRepository {
    Optional<StockPrice> findBySecurityIdAndPriceDate(Long securityId, LocalDate priceDate);

    // 해당 종목의 가장 최근 날짜 시세 1건 (없으면 empty)
    Optional<StockPrice> findLatestBySecurityId(Long securityId);

    StockPrice save(StockPrice stockPrice);
}
