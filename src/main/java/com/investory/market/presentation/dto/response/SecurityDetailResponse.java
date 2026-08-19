package com.investory.market.presentation.dto.response;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Security;
import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.services.dto.result.SecurityDetailResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SecurityDetailResponse(
        Long securityId,
        String securityCode,
        String securityName,
        MarketType marketType,
        String sectorName,
        String industryName,
        LatestPriceResponse latestPrice
) {
    public static SecurityDetailResponse from(SecurityDetailResult result) {
        Security security = result.security();
        return new SecurityDetailResponse(
                security.getSecurityId(),
                security.getSecurityCode(),
                security.getSecurityName(),
                security.getMarketType(),
                security.getSectorName(),
                security.getStdIdstClsfName(),
                LatestPriceResponse.from(result.latestPrice())
        );
    }

    public record LatestPriceResponse(
            LocalDate priceDate,
            Long openPrice,
            Long highPrice,
            Long lowPrice,
            Long closePrice,
            BigDecimal dailyReturnRate,
            Long tradingVolume,
            BigDecimal tradingValue
    ) {
        // securityPrice가 null이면(아직 저장된 시세가 없으면) latestPrice 자체를 null로 내려준다.
        public static LatestPriceResponse from(SecurityPrice securityPrice) {
            if (securityPrice == null) {
                return null;
            }
            return new LatestPriceResponse(
                    securityPrice.getPriceDate(),
                    securityPrice.getOpenPrice(),
                    securityPrice.getHighPrice(),
                    securityPrice.getLowPrice(),
                    securityPrice.getClosePrice(),
                    securityPrice.getDailyReturnRate(),
                    securityPrice.getTradingVolume(),
                    securityPrice.getTradingValue()
            );
        }
    }
}
