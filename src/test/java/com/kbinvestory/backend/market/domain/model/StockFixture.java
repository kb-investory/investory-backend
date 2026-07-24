package com.kbinvestory.backend.market.domain.model;

import com.kbinvestory.backend.market.domain.constant.MarketType;

import java.time.Instant;
import java.time.LocalDate;

public class StockFixture {

    public static Stock stock(String code, String name, MarketType market, String sector) {
        return Stock.of(null, code, name, market, sector, LocalDate.of(2020, 1, 1), Instant.now(), Instant.now());
    }
}