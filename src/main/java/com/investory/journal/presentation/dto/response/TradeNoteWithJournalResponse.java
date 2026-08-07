package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.TradeNoteWithJournalResult;

import java.time.Instant;
import java.time.LocalDate;

public record TradeNoteWithJournalResponse(
        Long journalTradeNoteId,
        Long journalId,
        LocalDate journalDate,
        String rationaleText,
        Instant createdAt,
        Instant updatedAt
) {
    public static TradeNoteWithJournalResponse from(TradeNoteWithJournalResult result) {
        return new TradeNoteWithJournalResponse(
                result.journalTradeNoteId(),
                result.journalId(),
                result.journalDate(),
                result.rationaleText(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
