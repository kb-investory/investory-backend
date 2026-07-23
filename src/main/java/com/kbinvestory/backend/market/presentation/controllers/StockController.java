package com.kbinvestory.backend.market.presentation.controllers;

import com.kbinvestory.backend.market.domain.services.MarketDataService;
import com.kbinvestory.backend.market.domain.services.dto.result.StockSearchResult;
import com.kbinvestory.backend.market.presentation.dto.request.StockSearchRequest;
import com.kbinvestory.backend.market.presentation.dto.response.StockSearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final MarketDataService marketDataService;

    public StockController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping
    public StockSearchResponse search(@ModelAttribute StockSearchRequest request) {
        StockSearchResult result = marketDataService.searchStocks(request.toQuery());
        return StockSearchResponse.from(result);
    }
}
