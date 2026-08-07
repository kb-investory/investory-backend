package com.investory.market.presentation.dto.response;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Stock;
import com.investory.market.domain.model.StockPrice;
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
        Stock stock = result.stock();
        return new SecurityDetailResponse(
                stock.getSecurityId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarketType(),
                stock.getStdIdstClsfCode(),
                stock.getStdIdstClsfName(),
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
        // stockPrice가 null이면(아직 저장된 시세가 없으면) latestPrice 자체를 null로 내려준다.
        public static LatestPriceResponse from(StockPrice stockPrice) {
            if (stockPrice == null) {
                return null;
            }
            return new LatestPriceResponse(
                    stockPrice.getPriceDate(),
                    stockPrice.getOpenPrice(),
                    stockPrice.getHighPrice(),
                    stockPrice.getLowPrice(),
                    stockPrice.getClosePrice(),
                    stockPrice.getDailyReturnRate(),
                    stockPrice.getTradingVolume(),
                    stockPrice.getTradingValue()
            );
        }
    }
}
