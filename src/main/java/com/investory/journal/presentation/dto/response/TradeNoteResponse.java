package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.TradeNoteResult;

import java.time.Instant;

public record TradeNoteResponse(
        Long journalTradeNoteId,
        String rationaleText,
        Instant createdAt,
        Instant updatedAt
) {
    public static TradeNoteResponse from(TradeNoteResult result) {
        return new TradeNoteResponse(
                result.journalTradeNoteId(),
                result.rationaleText(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
