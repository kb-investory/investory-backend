package com.investory.journal.domain.services.dto.result;

import java.time.LocalDate;
import java.util.List;

public record JournalDetailResult(
    LocalDate journalDate,
    boolean canCreate,
    JournalInfoResult journal,
    List<TradeDetailResult> trades
) {
}
