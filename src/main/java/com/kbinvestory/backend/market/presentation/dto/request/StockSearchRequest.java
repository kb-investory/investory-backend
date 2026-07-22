package com.kbinvestory.backend.market.presentation.dto.request;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockSearchRequest {
    private String keyword;
    private MarketType market;
    private String sector;
    private int page = 0;
    private int size = 20;

    public StockSearchQuery toQuery() {
        return new StockSearchQuery(keyword, market, sector, page, size);
    }
}