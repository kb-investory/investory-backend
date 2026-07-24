package com.kbinvestory.backend.market.domain.model;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.exception.MarketErrorCode;
import com.kbinvestory.backend.market.domain.exception.StockException;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
public class Stock {

    private final Long id;
    private final String code;
    private final String name;
    private final MarketType market;
    private final String sector;
    private final LocalDate listedDate;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Stock(Long id, String code, String name, MarketType market,
                  String sector, LocalDate listedDate, Instant createdAt, Instant updatedAt) {
        requireNonNull(code);
        requireNonNull(name);
        requireNonNull(market);
        requireNonNull(sector);
        requireNonNull(listedDate);
        requireNonNull(createdAt);
        requireNonNull(updatedAt);

        this.id = id;
        this.code = code;
        this.name = name;
        this.market = market;
        this.sector = sector;
        this.listedDate = listedDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new StockException(MarketErrorCode.INVALID_STOCK_DATA);
        }
    }

    // 신규 등록: id는 아직 없고, createdAt/updatedAt은 등록 시점으로 설정
    public static Stock create(String code, String name, MarketType market,
                                String sector, LocalDate listedDate) {
        Instant now = Instant.now();
        return new Stock(null, code, name, market, sector, listedDate, now, now);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static Stock of(Long id, String code, String name, MarketType market,
                           String sector, LocalDate listedDate, Instant createdAt, Instant updatedAt) {
        return new Stock(id, code, name, market, sector, listedDate, createdAt, updatedAt);
    }

    // 종목의 자연키(market, code)로 동등성을 판단한다 — 저장 전(id null)에도 같은 종목인지 비교 가능
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stock)) return false;
        Stock stock = (Stock) o;
        return market == stock.market && code.equals(stock.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(market, code);
    }
}