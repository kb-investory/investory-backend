package com.investory.market.presentation.dto.response;


import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Stock;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StockResponse(
        Long securityId,
        String stockCode,
        String stockName,
        MarketType marketType,
        String stdIdstClsfCode,
        String stdIdstClsfName,
        LocalDate listedDate,
        LocalDate delistedDate,
        boolean active,
        LocalDateTime updatedAt
) {
    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getSecurityId(), stock.getStockCode(), stock.getStockName(), stock.getMarketType(),
                stock.getStdIdstClsfCode(), stock.getStdIdstClsfName(),
                stock.getListedDate(), stock.getDelistedDate(), stock.isActive(), stock.getUpdatedAt()
        );
    }
}
