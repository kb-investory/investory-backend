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
                    stock.getStdIdstClsfCode(),
                    stock.getStdIdstClsfName()
            );
        }
    }
}
