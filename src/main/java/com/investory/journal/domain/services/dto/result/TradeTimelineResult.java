package com.investory.journal.domain.services.dto.result;

import java.util.List;

public record TradeTimelineResult(
    SecurityResult security,
    List<TradeTimelineEntryResult> trades,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
