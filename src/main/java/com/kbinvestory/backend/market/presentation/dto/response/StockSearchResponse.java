package com.kbinvestory.backend.market.presentation.dto.response;

import com.kbinvestory.backend.market.domain.services.dto.result.StockSearchResult;

import java.util.List;
import java.util.stream.Collectors;

public record StockSearchResponse(
    List<StockSummaryResponse> stocks,
    int page,
    int size,
    long totalCount,
    int totalPages
) {
    public static StockSearchResponse from(StockSearchResult result) {
        List<StockSummaryResponse> items = result.stocks().stream()
                .map(StockSummaryResponse::from)
                .collect(Collectors.toList());
        return new StockSearchResponse(items, result.page(), result.size(), result.totalCount(), result.totalPages());
    }
}
