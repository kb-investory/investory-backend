package com.investory.ledger.domain.services.dto.result;

import java.util.List;

public record TradeListResult(
    List<TradeResult> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
}
