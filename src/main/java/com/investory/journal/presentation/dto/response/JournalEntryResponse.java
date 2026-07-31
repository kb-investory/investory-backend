package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;

import java.time.Instant;
import java.time.LocalDate;

public record JournalEntryResponse(
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
    public static JournalEntryResponse from(JournalEntryResult result) {
        return new JournalEntryResponse(
                result.journalId(),
                result.journalDate(),
                result.marketMood(),
                result.tradeCount(),
                result.tradeNoteCount(),
                result.createdAt(),
                result.editableUntilAt(),
                result.isBackfilled(),
                result.isEditable()
        );
    }
}
