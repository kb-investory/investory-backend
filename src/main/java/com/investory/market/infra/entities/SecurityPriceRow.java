package com.investory.market.infra.entities;

import com.investory.market.domain.model.SecurityPrice;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SecurityPriceRow {
    private Long securityId;
    private LocalDate priceDate;
    private Long lowPrice;
    private Long highPrice;
    private Long openPrice;
    private Long closePrice;
    private BigDecimal dailyReturnRate;
    private Long tradingVolume;
    private BigDecimal tradingValue;
    private LocalDateTime createdAt;

    // SecurityPrice Domain을 DB에 저장할 수 있는 형태로 변환
    public static SecurityPriceRow from(SecurityPrice securityPrice) {
        SecurityPriceRow row = new SecurityPriceRow();
        row.securityId = securityPrice.getSecurityId();
        row.priceDate = securityPrice.getPriceDate();
        row.lowPrice = securityPrice.getLowPrice();
        row.highPrice = securityPrice.getHighPrice();
        row.openPrice = securityPrice.getOpenPrice();
        row.closePrice = securityPrice.getClosePrice();
        row.dailyReturnRate = securityPrice.getDailyReturnRate();
        row.tradingVolume = securityPrice.getTradingVolume();
        row.tradingValue = securityPrice.getTradingValue();
        row.createdAt = securityPrice.getCreatedAt();
        return row;
    }

    public SecurityPrice toDomain() {
        return SecurityPrice.of(
                securityId, priceDate, lowPrice,
                highPrice, openPrice, closePrice, dailyReturnRate,
                tradingVolume, tradingValue, createdAt
        );
    }
}
