package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.services.dto.result.JournalInfoResult;

import java.time.Instant;

public record JournalInfoResponse(
        Long journalId,
        String marketThought,
        MarketMood marketMood,
        Instant createdAt,
        Instant updatedAt,
        Instant editableUntilAt,
        boolean isBackfilled,
        boolean isEditable
) {
    public static JournalInfoResponse from(JournalInfoResult result) {
        return new JournalInfoResponse(
                result.journalId(),
                result.marketThought(),
                result.marketMood(),
                result.createdAt(),
                result.updatedAt(),
                result.editableUntilAt(),
                result.isBackfilled(),
                result.isEditable()
        );
    }
}
