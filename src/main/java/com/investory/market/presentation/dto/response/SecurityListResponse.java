package com.investory.market.presentation.dto.response;


import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Stock;
import com.investory.market.domain.services.dto.result.SecuritySearchResult;

import java.util.List;

public record SecurityListResponse(
        List<SecurityItem> securities,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static SecurityListResponse from(SecuritySearchResult result) {
        List<SecurityItem> items = result.stocks().stream()
                .map(SecurityItem::from)
                .toList();

        return new SecurityListResponse(
                items, result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }

    // sectorName에 대응하는 컬럼이 더 이상 없어서 항상 null로 내려간다. industryName은 industry_name 컬럼값이다.
    public record SecurityItem(
            Long securityId,
            String securityCode,
            String securityName,
            MarketType marketType,
            String sectorName,
            String industryName
    ) {
        public static SecurityItem from(Stock stock) {
            return new SecurityItem(
                    stock.getSecurityId(),
                    stock.getStockCode(),
                    stock.getStockName(),
                    stock.getMarketType(),
                    null,
                    stock.getStdIdstClsfName()
            );
        }
    }
}
