package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.MarketMood;

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
}
