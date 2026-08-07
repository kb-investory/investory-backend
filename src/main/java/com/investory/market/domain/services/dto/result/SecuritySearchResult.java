package com.investory.market.domain.services.dto.result;


import com.investory.market.domain.model.Stock;

import java.util.List;

// GET /market/securities 목록 조회 결과. totalPages는 서비스에서 totalElements/size로 계산해 채운다.
public record SecuritySearchResult(
        List<Stock> stocks,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
