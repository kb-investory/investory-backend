package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.services.dto.result.HoldingListResult;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record HoldingListResponse(
    LocalDate snapshotDate,
    HoldingSummaryResponse summary,
    List<HoldingResponse> holdings
) {
    public static HoldingListResponse from(HoldingListResult result) {
        List<HoldingResponse> holdings = result.holdings().stream()
                .map(HoldingResponse::from)
                .collect(Collectors.toList());
        return new HoldingListResponse(result.snapshotDate(), HoldingSummaryResponse.from(result.summary()), holdings);
    }
}
