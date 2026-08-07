package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.ports.dto.TradeTimelineInfo;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeTimelineEntryResult(
    Long tradeId,
    Long accountId,
    String accountName,
    TradeSide tradeSide,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal transactionCostAmount,
    Instant tradedAt,
    TradeNoteWithJournalResult note
) {
    public static TradeTimelineEntryResult from(TradeTimelineInfo trade, TradeNoteWithJournalResult note) {
        return new TradeTimelineEntryResult(
                trade.tradeId(),
                trade.accountId(),
                trade.accountName(),
                trade.tradeSide(),
                trade.quantity(),
                trade.unitPrice(),
                trade.transactionCostAmount(),
                trade.tradedAt(),
                note
        );
    }
}
