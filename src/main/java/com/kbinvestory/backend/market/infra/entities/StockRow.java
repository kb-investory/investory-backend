package com.kbinvestory.backend.market.infra.entities;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.model.Stock;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class StockRow {
    private Long stockId;
    private String stockCode;
    private String stockName;
    private MarketType market;
    private String sector;
    private LocalDate listedDate;
    private Instant createdAt;
    private Instant updatedAt;

    public Stock toDomain() {
        return Stock.of(stockId, stockCode, stockName, market, sector, listedDate, createdAt, updatedAt);
    }
}