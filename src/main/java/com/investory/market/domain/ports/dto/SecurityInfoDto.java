package com.investory.market.domain.ports.dto;

import com.investory.market.domain.constant.MarketType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SecurityInfoDto {

    /** 종목 코드 */
    private final String securityCode;

    /** 종목명 */
    private final String securityName;

    /** 시장 종류 */
    private final MarketType marketType;

    /** 업종 대분류 코드 (idx_bztp_lcls_cd) */
    private final String sectorCode;

    /** 업종 대분류명 (idx_bztp_lcls_cd_name) */
    private final String sectorName;

    /** 표준산업분류명 (std_idst_clsf_cd_name) */
    private final String stdIdstClsfName;

    /** 상장일 */
    private final LocalDate listedDate;

    /** 상장폐지일 */
    private final LocalDate delistedDate;

    /** 현재 상장 여부 */
    private final Boolean isActive;
}
