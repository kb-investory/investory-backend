package com.investory.market.domain.model;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.ports.dto.SecurityInfoDto;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class Security {
    private final Long securityId;        // 내부 고유 숫자 ID (PK, auto_increment)
    private final String securityCode;    // security_code
    private final String securityName;    // security_name
    private final MarketType marketType;  // market_type
    private final String sectorCode;      // sector_code (업종 대분류코드)
    private final String sectorName;      // sector_name (업종 대분류명)
    private final String stdIdstClsfName; // industry_name (표준산업분류명)
    private final LocalDate listedDate;   // 상장일
    private final LocalDate delistedDate; // 상장 폐지일
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Security(
            Long securityId, String securityCode, String securityName, MarketType marketType,
            String sectorCode, String sectorName, String stdIdstClsfName,
            LocalDate listedDate, LocalDate delistedDate,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.securityId = securityId;
        this.securityCode = securityCode;
        this.securityName = securityName;
        this.marketType = marketType;
        this.sectorCode = sectorCode;
        this.sectorName = sectorName;
        this.stdIdstClsfName = stdIdstClsfName;
        this.listedDate = listedDate;
        this.delistedDate = delistedDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // KIS API 조회 결과(SecurityInfoDto)로 신규/갱신 저장할 때 사용.
    // securityId는 DB가 채워준다(신규 insert 시 null로 시작).
    public static Security create(SecurityInfoDto dto) {
        LocalDateTime now = LocalDateTime.now();
        return new Security(
                null, dto.getSecurityCode(), dto.getSecurityName(), dto.getMarketType(),
                dto.getSectorCode(), dto.getSectorName(), dto.getStdIdstClsfName(),
                dto.getListedDate(), dto.getDelistedDate(),
                dto.getDelistedDate() == null, now, now
        );
    }

    // DB에서 조회한 값으로 도메인 객체를 복원할 때 사용
    public static Security of(
            Long securityId, String securityCode, String securityName, MarketType marketType,
            String sectorCode, String sectorName, String stdIdstClsfName,
            LocalDate listedDate, LocalDate delistedDate,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Security(
                securityId, securityCode, securityName, marketType,
                sectorCode, sectorName, stdIdstClsfName,
                listedDate, delistedDate, active, createdAt, updatedAt
        );
    }
}
