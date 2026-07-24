package com.kbinvestory.backend.market.domain.repositories;

import com.kbinvestory.backend.market.domain.model.Stock;
import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;

import java.util.List;

public interface StockRepository {
    List<Stock> search(StockSearchQuery query);
    long count(StockSearchQuery query);
}