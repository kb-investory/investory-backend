package com.investory.market.infra.entities;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Security;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SecurityRow {
    private Long securityId;
    private String securityCode;
    private String securityName;
    private String marketType;
    private String sectorCode;   // 업종 대분류코드 (idx_bztp_lcls_cd)
    private String sectorName;   // 업종 대분류명 (idx_bztp_lcls_cd_name)
    private String industryName; // 표준산업분류명 (std_idst_clsf_cd_name)
    private LocalDate listedDate;
    private LocalDate delistedDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Security Domain을 DB에 저장할 수 있는 형태로 변환
    public static SecurityRow from(Security security) {
        SecurityRow row = new SecurityRow();
        row.securityId = security.getSecurityId();
        row.securityCode = security.getSecurityCode();
        row.securityName = security.getSecurityName();
        row.marketType = security.getMarketType() != null ? security.getMarketType().name() : null;
        row.sectorCode = security.getSectorCode();
        row.sectorName = security.getSectorName();
        row.industryName = security.getStdIdstClsfName();
        row.listedDate = security.getListedDate();
        row.delistedDate = security.getDelistedDate();
        row.active = security.isActive();
        row.createdAt = security.getCreatedAt();
        row.updatedAt = security.getUpdatedAt();
        return row;
    }

    public Security toDomain() {
        return Security.of(
                securityId, securityCode, securityName,
                marketType != null ? MarketType.valueOf(marketType) : null,
                sectorCode, sectorName, industryName,
                listedDate, delistedDate, active, createdAt, updatedAt
        );
    }
}
