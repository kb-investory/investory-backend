package com.kbinvestory.backend.market.domain.services.dto.query;

import com.kbinvestory.backend.market.domain.constant.MarketType;

public record StockSearchQuery(
    String keyword,
    MarketType market,
    String sector,
    int page,
    int size
) {}