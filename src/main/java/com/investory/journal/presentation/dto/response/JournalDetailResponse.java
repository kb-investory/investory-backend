package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.JournalDetailResult;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record JournalDetailResponse(
        LocalDate journalDate,
        boolean canCreate,
        JournalInfoResponse journal,
        List<TradeDetailResponse> trades
) {
    public static JournalDetailResponse from(JournalDetailResult result) {
        JournalInfoResponse journal = result.journal() == null ? null : JournalInfoResponse.from(result.journal());
        List<TradeDetailResponse> trades = result.trades().stream()
                .map(TradeDetailResponse::from)
                .collect(Collectors.toList());
        return new JournalDetailResponse(result.journalDate(), result.canCreate(), journal, trades);
    }
}
