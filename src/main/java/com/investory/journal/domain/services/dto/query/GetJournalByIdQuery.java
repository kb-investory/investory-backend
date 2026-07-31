package com.investory.journal.domain.services.dto.query;

public record GetJournalByIdQuery(
    Long userId,
    Long journalId
) {
}
