package com.investory.journal.domain.services.dto.query;

import java.time.LocalDate;

public record GetTradeTimelineQuery(
    Long userId,
    Long securityId,
    LocalDate startDate,
    LocalDate endDate,
    int page,
    int size
) {
}
