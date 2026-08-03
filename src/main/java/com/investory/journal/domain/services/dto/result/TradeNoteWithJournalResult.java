package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.models.JournalTradeNote;

import java.time.Instant;
import java.time.LocalDate;

public record TradeNoteWithJournalResult(
    Long journalTradeNoteId,
    Long journalId,
    LocalDate journalDate,
    String rationaleText,
    Instant createdAt,
    Instant updatedAt
) {
    public static TradeNoteWithJournalResult from(JournalTradeNote note, LocalDate journalDate) {
        return new TradeNoteWithJournalResult(
                note.getJournalTradeNoteId(),
                note.getJournalId(),
                journalDate,
                note.getRationaleText(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
