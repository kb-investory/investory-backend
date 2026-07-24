package com.kbinvestory.backend.market.domain.services;

import com.kbinvestory.backend.market.domain.model.Stock;
import com.kbinvestory.backend.market.domain.repositories.StockRepository;
import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;
import com.kbinvestory.backend.market.domain.services.dto.result.StockResult;
import com.kbinvestory.backend.market.domain.services.dto.result.StockSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private final StockRepository stockRepository;

    public MarketDataService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public StockSearchResult searchStocks(StockSearchQuery query) {
        List<Stock> stocks = stockRepository.search(query);
        long totalCount = stockRepository.count(query);
        List<StockResult> results = stocks.stream()
                .map(StockResult::from)
                .collect(Collectors.toList());
        return new StockSearchResult(results, query.page(), query.size(), totalCount);
    }
}