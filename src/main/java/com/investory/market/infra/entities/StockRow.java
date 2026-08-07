package com.investory.market.infra.entities;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Stock;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class StockRow {
    private Long securityId;
    private String stockCode;
    private String stockName;
    private String marketType;
    private String sectorName;   // DB 컬럼명은 sector_name이지만 실제로는 산업분류 코드(std_idst_clsf_cd)가 들어간다
    private String industryName; // 산업분류명 (std_idst_clsf_cd_name)
    private LocalDate listedDate;
    private LocalDate delistedDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Stock Domain을 DB에 저장할 수 있는 형태로 변환
    public static StockRow from(Stock stock) {
        StockRow row = new StockRow();
        row.securityId = stock.getSecurityId();
        row.stockCode = stock.getStockCode();
        row.stockName = stock.getStockName();
        row.marketType = stock.getMarketType() != null ? stock.getMarketType().name() : null;
        row.sectorName = stock.getStdIdstClsfCode();
        row.industryName = stock.getStdIdstClsfName();
        row.listedDate = stock.getListedDate();
        row.delistedDate = stock.getDelistedDate();
        row.active = stock.isActive();
        row.createdAt = stock.getCreatedAt();
        row.updatedAt = stock.getUpdatedAt();
        return row;
    }

    public Stock toDomain() {
        return Stock.of(
                securityId, stockCode, stockName,
                marketType != null ? MarketType.valueOf(marketType) : null,
                sectorName, industryName,
                listedDate, delistedDate, active, createdAt, updatedAt
        );
    }
}
