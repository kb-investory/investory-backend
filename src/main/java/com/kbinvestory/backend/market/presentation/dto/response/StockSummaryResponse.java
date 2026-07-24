package com.kbinvestory.backend.market.presentation.dto.response;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.services.dto.result.StockResult;

import java.time.LocalDate;

public record StockSummaryResponse(
    Long id,
    String code,
    String name,
    MarketType market,
    String sector,
    LocalDate listedDate
) {
    public static StockSummaryResponse from(StockResult result) {
        return new StockSummaryResponse(
                result.id(),
                result.code(),
                result.name(),
                result.market(),
                result.sector(),
                result.listedDate()
        );
    }
}