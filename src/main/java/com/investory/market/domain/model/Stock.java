package com.investory.market.domain.model;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.ports.dto.StockInfoDto;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class Stock {
    private final Long securityId;        // 내부 고유 숫자 ID (PK, auto_increment)
    private final String stockCode;       // security_code
    private final String stockName;       // security_name
    private final MarketType marketType;  // market_type
    private final String stdIdstClsfCode; // industry_code (표준산업분류코드)
    private final String stdIdstClsfName; // industry_name (표준산업분류명)
    private final LocalDate listedDate;   // 상장일
    private final LocalDate delistedDate; // 상장 폐지일
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Stock(
            Long securityId, String stockCode, String stockName, MarketType marketType,
            String stdIdstClsfCode, String stdIdstClsfName,
            LocalDate listedDate, LocalDate delistedDate,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.securityId = securityId;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.marketType = marketType;
        this.stdIdstClsfCode = stdIdstClsfCode;
        this.stdIdstClsfName = stdIdstClsfName;
        this.listedDate = listedDate;
        this.delistedDate = delistedDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // KIS API 조회 결과(StockInfoDto)로 신규/갱신 저장할 때 사용.
    // securityId는 DB가 채워준다(신규 insert 시 null로 시작).
    public static Stock create(StockInfoDto dto) {
        LocalDateTime now = LocalDateTime.now();
        return new Stock(
                null, dto.getStockCode(), dto.getStockName(), dto.getMarketType(),
                dto.getStdIdstClsfCode(), dto.getStdIdstClsfName(),
                dto.getListedDate(), dto.getDelistedDate(),
                dto.getDelistedDate() == null, now, now
        );
    }

    // DB에서 조회한 값으로 도메인 객체를 복원할 때 사용
    public static Stock of(
            Long securityId, String stockCode, String stockName, MarketType marketType,
            String stdIdstClsfCode, String stdIdstClsfName,
            LocalDate listedDate, LocalDate delistedDate,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Stock(
                securityId, stockCode, stockName, marketType,
                stdIdstClsfCode, stdIdstClsfName,
                listedDate, delistedDate, active, createdAt, updatedAt
        );
    }
}
