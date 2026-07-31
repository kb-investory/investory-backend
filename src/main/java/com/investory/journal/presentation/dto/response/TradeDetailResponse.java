package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.services.dto.result.TradeDetailResult;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeDetailResponse(
        Long tradeId,
        Long securityId,
        String securityCode,
        String securityName,
        TradeSide tradeSide,
        int quantity,
        BigDecimal unitPrice,
        Instant tradedAt,
        TradeNoteResponse note
) {
    public static TradeDetailResponse from(TradeDetailResult result) {
        TradeNoteResponse note = result.note() == null ? null : TradeNoteResponse.from(result.note());
        return new TradeDetailResponse(
                result.tradeId(),
                result.securityId(),
                result.securityCode(),
                result.securityName(),
                result.tradeSide(),
                result.quantity(),
                result.unitPrice(),
                result.tradedAt(),
                note
        );
    }
}
