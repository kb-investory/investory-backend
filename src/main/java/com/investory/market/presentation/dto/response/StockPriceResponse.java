package com.investory.market.presentation.dto.response;


import com.investory.market.domain.model.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockPriceResponse(
        Long securityId,
        LocalDate priceDate,
        Long lowPrice,
        Long highPrice,
        Long openPrice,
        Long closePrice,
        BigDecimal dailyReturnRate,
        Long tradingVolume,
        Long tradingValue
) {
    public static StockPriceResponse from(StockPrice stockPrice) {
        return new StockPriceResponse(
                stockPrice.getSecurityId(), stockPrice.getPriceDate(),
                stockPrice.getLowPrice(), stockPrice.getHighPrice(),
                stockPrice.getOpenPrice(), stockPrice.getClosePrice(),
                stockPrice.getDailyReturnRate(), stockPrice.getTradingVolume(), stockPrice.getTradingValue()
        );
    }
}
