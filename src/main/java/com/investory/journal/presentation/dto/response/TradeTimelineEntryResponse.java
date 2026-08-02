package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.services.dto.result.TradeTimelineEntryResult;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeTimelineEntryResponse(
        Long tradeId,
        Long accountId,
        String accountName,
        TradeSide tradeSide,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal transactionCostAmount,
        Instant tradedAt,
        TradeNoteWithJournalResponse note
) {
    public static TradeTimelineEntryResponse from(TradeTimelineEntryResult result) {
        TradeNoteWithJournalResponse note = result.note() == null ? null : TradeNoteWithJournalResponse.from(result.note());
        return new TradeTimelineEntryResponse(
                result.tradeId(),
                result.accountId(),
                result.accountName(),
                result.tradeSide(),
                result.quantity(),
                result.unitPrice(),
                result.transactionCostAmount(),
                result.tradedAt(),
                note
        );
    }
}
