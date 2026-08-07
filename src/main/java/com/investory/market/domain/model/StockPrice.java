package com.investory.market.domain.model;

import com.investory.market.domain.ports.dto.StockPriceDto;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class StockPrice {
    private final Long securityId;
    private final LocalDate priceDate;
    private final Long lowPrice;
    private final Long highPrice;
    private final Long openPrice;
    private final Long closePrice;
    private final BigDecimal dailyReturnRate;
    private final Long tradingVolume;
    private final BigDecimal tradingValue;
    private final LocalDateTime createdAt;

    private StockPrice(
            Long securityId, LocalDate priceDate, Long lowPrice,
            Long highPrice, Long openPrice, Long closePrice, BigDecimal dailyReturnRate,
            Long tradingVolume, BigDecimal tradingValue, LocalDateTime createdAt) {
        this.securityId = securityId;
        this.priceDate = priceDate;
        this.lowPrice = lowPrice;
        this.highPrice = highPrice;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.dailyReturnRate = dailyReturnRate;
        this.tradingVolume = tradingVolume;
        this.tradingValue = tradingValue;
        this.createdAt = createdAt;
    }

    // KIS API 조회 결과(StockPriceDto)로 신규 저장할 때 사용.
    // PK가 (security_id, price_date) 복합키라 별도 surrogate id는 없다.
    // dto에 priceDate가 없으면(당일 현재가 조회 응답) 오늘 날짜로 채운다.
    public static StockPrice create(Long securityId, StockPriceDto dto) {
        LocalDate priceDate = dto.getPriceDate() != null ? dto.getPriceDate() : LocalDate.now();
        BigDecimal tradingValue = dto.getTradingValue() != null
                ? BigDecimal.valueOf(dto.getTradingValue())
                : null;
        return new StockPrice(
                securityId, priceDate, dto.getLowPrice(),
                dto.getHighPrice(), dto.getOpenPrice(), dto.getClosePrice(), dto.getDailyReturnRate(),
                dto.getTradingVolume(), tradingValue, LocalDateTime.now()
        );
    }

    // DB에서 조회한 값으로 도메인 객체를 복원할 때 사용
    public static StockPrice of(
            Long securityId, LocalDate priceDate, Long lowPrice,
            Long highPrice, Long openPrice, Long closePrice, BigDecimal dailyReturnRate,
            Long tradingVolume, BigDecimal tradingValue, LocalDateTime createdAt) {
        return new StockPrice(
                securityId, priceDate, lowPrice,
                highPrice, openPrice, closePrice, dailyReturnRate,
                tradingVolume, tradingValue, createdAt
        );
    }
}
