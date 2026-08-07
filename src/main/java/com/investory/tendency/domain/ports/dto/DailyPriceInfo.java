package com.investory.tendency.domain.ports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPriceInfo(
    LocalDate priceDate,
    BigDecimal closePrice
) {
}
