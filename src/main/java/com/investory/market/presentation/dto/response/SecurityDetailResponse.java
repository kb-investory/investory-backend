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
    // sectorName에 대응하는 별도 컬럼이 더 이상 없어서(업종 대/중분류 컬럼 폐지) 항상 null로 내려간다.
    // industryName은 industry_name(표준산업분류명) 컬럼값이다.
    public static SecurityDetailResponse from(SecurityDetailResult result) {
        Stock stock = result.stock();
        return new SecurityDetailResponse(
                stock.getSecurityId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarketType(),
                null,
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
            Long tradingValue
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
