package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.MarketMood;

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
}
