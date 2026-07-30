package com.investory.journal.domain.services.dto.query;

import java.time.LocalDate;

public record GetJournalDetailQuery(
    Long userId,
    LocalDate date
) {
}
