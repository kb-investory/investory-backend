package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.services.dto.result.TradeListResult;

import java.util.List;
import java.util.stream.Collectors;

public record TradeListResponse(
    List<TradeResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
    public static TradeListResponse from(TradeListResult result) {
        List<TradeResponse> content = result.content().stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
        return new TradeListResponse(content, result.page(), result.size(), result.totalElements(),
                result.totalPages(), result.hasNext());
    }
}
