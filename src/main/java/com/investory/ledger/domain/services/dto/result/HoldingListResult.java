package com.investory.ledger.domain.services.dto.result;

import java.time.LocalDate;
import java.util.List;

public record HoldingListResult(
    LocalDate snapshotDate,
    HoldingSummaryResult summary,
    List<HoldingResult> holdings
) {
}
