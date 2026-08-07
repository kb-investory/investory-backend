package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.models.JournalTradeNote;

import java.time.Instant;

public record TradeNoteResult(
    Long journalTradeNoteId,
    String rationaleText,
    Instant createdAt,
    Instant updatedAt
) {
    public static TradeNoteResult from(JournalTradeNote note) {
        return new TradeNoteResult(
                note.getJournalTradeNoteId(),
                note.getRationaleText(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
