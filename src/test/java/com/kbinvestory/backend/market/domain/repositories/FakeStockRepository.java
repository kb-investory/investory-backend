package com.kbinvestory.backend.market.domain.repositories;

import com.kbinvestory.backend.market.domain.model.Stock;
import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeStockRepository implements StockRepository {

    private final List<Stock> stocks = new ArrayList<>();

    public void add(Stock... stocks) {
        this.stocks.addAll(List.of(stocks));
    }

    @Override
    public List<Stock> search(StockSearchQuery query) {
        List<Stock> filtered = filter(query);
        int fromIndex = Math.min(query.page() * query.size(), filtered.size());
        int toIndex = Math.min(fromIndex + query.size(), filtered.size());
        return filtered.subList(fromIndex, toIndex);
    }

    @Override
    public long count(StockSearchQuery query) {
        return filter(query).size();
    }

    private List<Stock> filter(StockSearchQuery query) {
        return stocks.stream()
                .filter(stock -> matchesKeyword(stock, query.keyword()))
                .filter(stock -> query.market() == null || stock.getMarket() == query.market())
                .filter(stock -> matchesSector(stock, query.sector()))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(Stock stock, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return stock.getCode().contains(keyword) || stock.getName().contains(keyword);
    }

    private boolean matchesSector(Stock stock, String sector) {
        if (sector == null || sector.isEmpty()) {
            return true;
        }
        return sector.equals(stock.getSector());
    }
}