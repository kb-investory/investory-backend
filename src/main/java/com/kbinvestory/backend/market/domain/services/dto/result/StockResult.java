package com.kbinvestory.backend.market.domain.services.dto.result;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.model.Stock;

import java.time.LocalDate;

public record StockResult(
    Long id,
    String code,
    String name,
    MarketType market,
    String sector,
    LocalDate listedDate
) {
    public static StockResult from(Stock stock) {
        return new StockResult(
                stock.getId(),
                stock.getCode(),
                stock.getName(),
                stock.getMarket(),
                stock.getSector(),
                stock.getListedDate()
        );
    }
}