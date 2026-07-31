package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.models.Journal;

import java.time.Instant;
import java.time.LocalDate;

public record JournalEntryResult(
    Long journalId,
    LocalDate journalDate,
    MarketMood marketMood,
    int tradeCount,
    int tradeNoteCount,
    Instant createdAt,
    Instant editableUntilAt,
    boolean isBackfilled,
    boolean isEditable
) {
    public static JournalEntryResult from(Journal journal, int tradeCount, Instant now) {
        return new JournalEntryResult(
                journal.getJournalId(),
                journal.getJournalDate(),
                journal.getMarketMood(),
                tradeCount,
                journal.getTradeNoteCount(),
                journal.getCreatedAt(),
                journal.getEditableUntilAt(),
                journal.isBackfilled(),
                journal.isEditable(now)
        );
    }
}
