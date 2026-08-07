package com.investory.journal.domain.services.dto.result;

import java.time.Instant;

public record CreateJournalResult(
    Long journalId,
    Instant createdAt
) {
}
