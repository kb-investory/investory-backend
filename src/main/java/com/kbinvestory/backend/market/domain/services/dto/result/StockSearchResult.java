package com.kbinvestory.backend.market.domain.services.dto.result;

import java.util.List;

public record StockSearchResult(
    List<StockResult> stocks,
    int page,
    int size,
    long totalCount
) {
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalCount / size);
    }
}