package com.investory.market.infra.entities;

import com.investory.market.domain.model.StockPrice;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class StockPriceRow {
    private Long priceId;
    private Long securityId;
    private LocalDate priceDate;
    private Long lowPrice;
    private Long highPrice;
    private Long openPrice;
    private Long closePrice;
    private BigDecimal dailyReturnRate;
    private Long tradingVolume;
    private Long tradingValue;
    private LocalDateTime createdAt;

    // StockPrice Domain을 DB에 저장할 수 있는 형태로 변환
    public static StockPriceRow from(StockPrice stockPrice) {
        StockPriceRow row = new StockPriceRow();
        row.priceId = stockPrice.getPriceId();
        row.securityId = stockPrice.getSecurityId();
        row.priceDate = stockPrice.getPriceDate();
        row.lowPrice = stockPrice.getLowPrice();
        row.highPrice = stockPrice.getHighPrice();
        row.openPrice = stockPrice.getOpenPrice();
        row.closePrice = stockPrice.getClosePrice();
        row.dailyReturnRate = stockPrice.getDailyReturnRate();
        row.tradingVolume = stockPrice.getTradingVolume();
        row.tradingValue = stockPrice.getTradingValue();
        row.createdAt = stockPrice.getCreatedAt();
        return row;
    }

    public StockPrice toDomain() {
        return StockPrice.of(
                priceId, securityId, priceDate, lowPrice,
                highPrice, openPrice, closePrice, dailyReturnRate,
                tradingVolume, tradingValue, createdAt
        );
    }
}
