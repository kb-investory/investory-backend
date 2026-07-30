package com.investory.journal.domain.services.dto.result;

import java.time.Instant;

public record TradeNoteResult(
    Long journalTradeNoteId,
    String rationaleText,
    Instant createdAt,
    Instant updatedAt
) {
}
