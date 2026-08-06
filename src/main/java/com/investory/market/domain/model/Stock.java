package com.investory.market.domain.model;

import com.kbinvestory.backend.market.domain.constant.MarketType;
import com.kbinvestory.backend.market.domain.ports.dto.StockInfoDto;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class Stock {
    private final String securityId;
    private final String stockCode;
    private final String stockName;
    private final MarketType marketType;
    private final String idxBztpLclsCode; // 지수업종대분류코드
    private final String idxBztpLclsName; // 지수업종대분류코드명
    private final String idxBztpMclsCode; // 지수업종중분류코드
    private final String idxBztpMclsName; // 지수업종중분류코드명
    private final String idxBztpSclsCode; // 지수업종소분류코드
    private final String idxBztpSclsName; // 지수업종소분류코드명
    private final String stdIdstClsfCode; // 표준산업분류코드
    private final String stdIdstClsfName; // 표준산업분류코드명
    private final LocalDate listedDate;   // 상장일
    private final LocalDate delistedDate; // 상장 폐지일
    private final boolean active;
    private final LocalDateTime updatedAt;

    private Stock(
            String securityId, String stockCode,
            String stockName, MarketType marketType,
            String idxBztpLclsCode, String idxBztpLclsName,
            String idxBztpMclsCode, String idxBztpMclsName,
            String idxBztpSclsCode, String idxBztpSclsName,
            String stdIdstClsfCode, String stdIdstClsfName,
            LocalDate listedDate, LocalDate delistedDate,
            boolean active, LocalDateTime updatedAt) {
        this.securityId = securityId;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.marketType = marketType;
        this.idxBztpLclsCode = idxBztpLclsCode;
        this.idxBztpLclsName = idxBztpLclsName;
        this.idxBztpMclsCode = idxBztpMclsCode;
        this.idxBztpMclsName = idxBztpMclsName;
        this.idxBztpSclsCode = idxBztpSclsCode;
        this.idxBztpSclsName = idxBztpSclsName;
        this.stdIdstClsfCode = stdIdstClsfCode;
        this.stdIdstClsfName = stdIdstClsfName;
        this.listedDate = listedDate;
        this.delistedDate = delistedDate;
        this.active = active;
        this.updatedAt = updatedAt;
    }

    // KIS API 조회 결과(StockInfoDto)로 신규/갱신 저장할 때 사용
    public static Stock create(StockInfoDto dto) {
        return new Stock(
                dto.getSecurityId(), dto.getStockCode(),
                dto.getStockName(), dto.getMarketType(),
                dto.getIdxBztpLclsCode(), dto.getIdxBztpLclsName(),
                dto.getIdxBztpMclsCode(), dto.getIdxBztpMclsName(),
                dto.getIdxBztpSclsCode(), dto.getIdxBztpSclsName(),
                dto.getStdIdstClsfCode(), dto.getStdIdstClsfName(),
                dto.getListedDate(), dto.getDelistedDate(),
                dto.getDelistedDate() == null, LocalDateTime.now()
        );
    }

    // DB에서 조회한 값으로 도메인 객체를 복원할 때 사용
    public static Stock of(
            String securityId, String stockCode,
            String stockName, MarketType marketType,
            String idxBztpLclsCode, String idxBztpLclsName,
            String idxBztpMclsCode, String idxBztpMclsName,
            String idxBztpSclsCode, String idxBztpSclsName,
            String stdIdstClsfCode, String stdIdstClsfName,
            LocalDate listedDate, LocalDate delistedDate,
            boolean active, LocalDateTime updatedAt) {
        return new Stock(
                securityId, stockCode, stockName, marketType,
                idxBztpLclsCode, idxBztpLclsName, idxBztpMclsCode, idxBztpMclsName,
                idxBztpSclsCode, idxBztpSclsName, stdIdstClsfCode, stdIdstClsfName,
                listedDate, delistedDate, active, updatedAt
        );
    }
}
