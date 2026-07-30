package com.investory.journal.domain.ports.dto;

import java.time.LocalDate;

public record TradeCountInfo(
    LocalDate tradeDate,
    int tradeCount
) {
}
