package com.kbinvestory.backend.market.domain.services;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.model.StockFixture;
import com.kbinvestory.backend.market.domain.repositories.FakeStockRepository;
import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;
import com.kbinvestory.backend.market.domain.services.dto.result.StockSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDataServiceTest {

    private FakeStockRepository stockRepository;
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        stockRepository = new FakeStockRepository();
        marketDataService = new MarketDataService(stockRepository);
    }

    @Test
    void 키워드로_종목명과_코드를_모두_검색한다() {
        stockRepository.add(
                StockFixture.stock("005930", "삼성전자", MarketType.KOSPI, "전기전자"),
                StockFixture.stock("000660", "SK하이닉스", MarketType.KOSPI, "전기전자"),
                StockFixture.stock("035720", "카카오", MarketType.KOSDAQ, "서비스업")
        );

        StockSearchResult result = marketDataService.searchStocks(
                new StockSearchQuery("삼성", null, null, 0, 20));

        assertEquals(1, result.stocks().size());
        assertEquals("005930", result.stocks().get(0).code());
    }

    @Test
    void 마켓과_섹터로_필터링한다() {
        stockRepository.add(
                StockFixture.stock("005930", "삼성전자", MarketType.KOSPI, "전기전자"),
                StockFixture.stock("035720", "카카오", MarketType.KOSDAQ, "서비스업"),
                StockFixture.stock("051910", "LG화학", MarketType.KOSPI, "화학")
        );

        StockSearchResult result = marketDataService.searchStocks(
                new StockSearchQuery(null, MarketType.KOSPI, "화학", 0, 20));

        assertEquals(1, result.stocks().size());
        assertEquals("051910", result.stocks().get(0).code());
    }

    @Test
    void 조건에_맞는_종목이_없으면_빈_결과를_반환한다() {
        stockRepository.add(StockFixture.stock("005930", "삼성전자", MarketType.KOSPI, "전기전자"));

        StockSearchResult result = marketDataService.searchStocks(
                new StockSearchQuery("없는종목", null, null, 0, 20));

        assertTrue(result.stocks().isEmpty());
        assertEquals(0, result.totalCount());
    }

    @Test
    void 나누어떨어지지_않는_전체_개수는_totalPages를_올림한다() {
        for (int i = 0; i < 5; i++) {
            stockRepository.add(StockFixture.stock("00000" + i, "종목" + i, MarketType.KOSPI, "업종"));
        }

        StockSearchResult result = marketDataService.searchStocks(
                new StockSearchQuery(null, null, null, 0, 2));

        assertEquals(2, result.stocks().size());
        assertEquals(5, result.totalCount());
        assertEquals(3, result.totalPages());
    }

    @Test
    void 마지막_페이지는_남은_개수만큼만_반환한다() {
        for (int i = 0; i < 5; i++) {
            stockRepository.add(StockFixture.stock("00000" + i, "종목" + i, MarketType.KOSPI, "업종"));
        }

        StockSearchResult result = marketDataService.searchStocks(
                new StockSearchQuery(null, null, null, 2, 2));

        assertEquals(1, result.stocks().size());
    }
}