package com.investory.market.presentation.dto.response;


import com.investory.market.domain.model.SecurityPrice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SecurityPriceResponse(
        Long securityId,
        LocalDate priceDate,
        Long lowPrice,
        Long highPrice,
        Long openPrice,
        Long closePrice,
        BigDecimal dailyReturnRate,
        Long tradingVolume,
        BigDecimal tradingValue
) {
    public static SecurityPriceResponse from(SecurityPrice securityPrice) {
        return new SecurityPriceResponse(
                securityPrice.getSecurityId(), securityPrice.getPriceDate(),
                securityPrice.getLowPrice(), securityPrice.getHighPrice(),
                securityPrice.getOpenPrice(), securityPrice.getClosePrice(),
                securityPrice.getDailyReturnRate(), securityPrice.getTradingVolume(), securityPrice.getTradingValue()
        );
    }
}
