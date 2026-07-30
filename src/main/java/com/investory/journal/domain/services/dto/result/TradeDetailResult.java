package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeDetailResult(
    Long tradeId,
    Long securityId,
    String securityCode,
    String securityName,
    TradeSide tradeSide,
    int quantity,
    BigDecimal unitPrice,
    Instant tradedAt,
    TradeNoteResult note
) {
}
