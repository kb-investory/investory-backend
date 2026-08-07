package com.investory.journal.domain.services.dto.query;

import java.time.LocalDate;

public record GetJournalEntriesQuery(
    Long userId,
    LocalDate startDate,
    LocalDate endDate
) {
}
