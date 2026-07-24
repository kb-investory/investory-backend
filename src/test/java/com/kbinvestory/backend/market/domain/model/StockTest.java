package com.kbinvestory.backend.market.domain.model;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.exception.MarketErrorCode;
import com.kbinvestory.backend.market.domain.exception.StockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockTest {

    private static final String CODE = "005930";
    private static final String NAME = "삼성전자";
    private static final MarketType MARKET = MarketType.KOSPI;
    private static final String SECTOR = "전기전자";
    private static final LocalDate LISTED_DATE = LocalDate.of(2020, 1, 1);
    private static final Instant NOW = Instant.now();

    @Test
    void create는_id없이_생성되고_createdAt과_updatedAt이_같은_시점으로_설정된다() {
        Stock stock = Stock.create(CODE, NAME, MARKET, SECTOR, LISTED_DATE);

        assertNull(stock.getId());
        assertEquals(CODE, stock.getCode());
        assertEquals(stock.getCreatedAt(), stock.getUpdatedAt());
    }

    @Test
    void create는_code가_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.create(null, NAME, MARKET, SECTOR, LISTED_DATE));
    }

    @Test
    void create는_name이_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.create(CODE, null, MARKET, SECTOR, LISTED_DATE));
    }

    @Test
    void create는_market이_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.create(CODE, NAME, null, SECTOR, LISTED_DATE));
    }

    @Test
    void create는_sector가_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.create(CODE, NAME, MARKET, null, LISTED_DATE));
    }

    @Test
    void create는_listedDate가_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.create(CODE, NAME, MARKET, SECTOR, null));
    }

    @Test
    void of는_영속화된_값으로_그대로_복원된다() {
        Stock stock = Stock.of(1L, CODE, NAME, MARKET, SECTOR, LISTED_DATE, NOW, NOW);

        assertEquals(1L, stock.getId());
        assertEquals(CODE, stock.getCode());
        assertEquals(NAME, stock.getName());
        assertEquals(MARKET, stock.getMarket());
        assertEquals(SECTOR, stock.getSector());
        assertEquals(LISTED_DATE, stock.getListedDate());
        assertEquals(NOW, stock.getCreatedAt());
        assertEquals(NOW, stock.getUpdatedAt());
    }

    @Test
    void of는_id가_null이어도_생성된다() {
        Stock stock = Stock.of(null, CODE, NAME, MARKET, SECTOR, LISTED_DATE, NOW, NOW);

        assertNull(stock.getId());
    }

    @Test
    void of는_code가_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, null, NAME, MARKET, SECTOR, LISTED_DATE, NOW, NOW));
    }

    @Test
    void of는_name이_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, CODE, null, MARKET, SECTOR, LISTED_DATE, NOW, NOW));
    }

    @Test
    void of는_market이_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, CODE, NAME, null, SECTOR, LISTED_DATE, NOW, NOW));
    }

    @Test
    void of는_sector가_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, CODE, NAME, MARKET, null, LISTED_DATE, NOW, NOW));
    }

    @Test
    void of는_listedDate가_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, CODE, NAME, MARKET, SECTOR, null, NOW, NOW));
    }

    @Test
    void of는_createdAt이_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, CODE, NAME, MARKET, SECTOR, LISTED_DATE, null, NOW));
    }

    @Test
    void of는_updatedAt이_null이면_예외를_던진다() {
        assertInvalidStockData(() -> Stock.of(1L, CODE, NAME, MARKET, SECTOR, LISTED_DATE, NOW, null));
    }

    @Test
    void market과_code가_같으면_동등하다() {
        Stock stock1 = Stock.of(1L, CODE, NAME, MARKET, SECTOR, LISTED_DATE, NOW, NOW);
        Stock stock2 = Stock.of(2L, CODE, "삼성전자우", MARKET, "다른섹터", LISTED_DATE.plusDays(1), Instant.now(), Instant.now());

        assertEquals(stock1, stock2);
        assertEquals(stock1.hashCode(), stock2.hashCode());
    }

    @Test
    void code가_같아도_market이_다르면_동등하지_않다() {
        Stock stock1 = Stock.of(1L, CODE, NAME, MarketType.KOSPI, SECTOR, LISTED_DATE, NOW, NOW);
        Stock stock2 = Stock.of(1L, CODE, NAME, MarketType.KOSDAQ, SECTOR, LISTED_DATE, NOW, NOW);

        assertNotEquals(stock1, stock2);
    }

    @Test
    void id가_null인_저장_전_인스턴스끼리도_market과_code가_같으면_동등하다() {
        Stock stock1 = Stock.create(CODE, NAME, MARKET, SECTOR, LISTED_DATE);
        Stock stock2 = Stock.create(CODE, NAME, MARKET, SECTOR, LISTED_DATE);

        assertEquals(stock1, stock2);
    }

    private void assertInvalidStockData(Executable executable) {
        StockException exception = assertThrows(StockException.class, executable);
        assertEquals(MarketErrorCode.INVALID_STOCK_DATA, exception.getErrorCode());
    }
}