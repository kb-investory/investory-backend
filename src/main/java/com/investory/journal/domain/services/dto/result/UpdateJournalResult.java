package com.investory.journal.domain.services.dto.result;

import java.time.Instant;

public record UpdateJournalResult(
    Long journalId,
    Instant updatedAt
) {
}
