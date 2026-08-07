package com.investory.market.domain.ports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StockPriceDto {

    /** 종목 식별자 */
    private final String securityId;

    /** 가격 기준일 */
    private final LocalDate priceDate;

    /** 저가 */
    private final Long lowPrice;

    /** 고가 */
    private final Long highPrice;

    /** 시가 */
    private final Long openPrice;

    /** 종가(현재가/장 종료 후 종가) */
    private final Long closePrice;

    /** 등락률(%) */
    private final BigDecimal dailyReturnRate;

    /** 거래량 */
    private final Long tradingVolume;

    /** 거래대금 */
    private final Long tradingValue;

    /** 생성 시각 */
    private final LocalDateTime createdAt;
}