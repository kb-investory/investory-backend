package com.investory.tendency.domain.ports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPriceInfo(
    LocalDate priceDate,
    BigDecimal closePrice,
    BigDecimal dailyReturnRate   // 전일대비 등락률(%). 데이터 없으면 null일 수 있음.
) {
}
