package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.TradeTimelineResult;

import java.util.List;
import java.util.stream.Collectors;

public record TradeTimelineResponse(
        SecurityResponse security,
        List<TradeTimelineEntryResponse> trades,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static TradeTimelineResponse from(TradeTimelineResult result) {
        List<TradeTimelineEntryResponse> trades = result.trades().stream()
                .map(TradeTimelineEntryResponse::from)
                .collect(Collectors.toList());
        return new TradeTimelineResponse(
                SecurityResponse.from(result.security()),
                trades,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
