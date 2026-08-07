package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.UpdateJournalResult;

import java.time.Instant;

public record UpdateJournalResponse(Long journalId, Instant updatedAt) {
    public static UpdateJournalResponse from(UpdateJournalResult result) {
        return new UpdateJournalResponse(result.journalId(), result.updatedAt());
    }
}
