package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.models.Journal;

import java.time.Instant;

public record JournalInfoResult(
    Long journalId,
    String marketThought,
    MarketMood marketMood,
    Instant createdAt,
    Instant updatedAt,
    Instant editableUntilAt,
    boolean isBackfilled,
    boolean isEditable
) {
    public static JournalInfoResult from(Journal journal, Instant now) {
        return new JournalInfoResult(
                journal.getJournalId(),
                journal.getMarketThought(),
                journal.getMarketMood(),
                journal.getCreatedAt(),
                journal.getUpdatedAt(),
                journal.getEditableUntilAt(),
                journal.isBackfilled(),
                journal.isEditable(now)
        );
    }
}
