package com.investory.ledger.domain.services.dto.command;

import java.time.LocalDate;
import java.util.List;

public record IngestRawHoldingsCommand(
    Long userId,
    Long accountId,
    LocalDate baseDate,
    List<RawHoldingRecord> rawHoldings
) {
}
