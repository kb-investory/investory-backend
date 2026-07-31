package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.CreateJournalResult;

import java.time.Instant;

public record CreateJournalResponse(Long journalId, Instant createdAt) {
    public static CreateJournalResponse from(CreateJournalResult result) {
        return new CreateJournalResponse(result.journalId(), result.createdAt());
    }
}
