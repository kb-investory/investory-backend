package com.investory.market.domain.ports.dto;

import com.investory.market.domain.constant.MarketType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StockInfoDto {

    /** 서비스 내부 종목 식별자 */
    private final String securityId;

    /** 종목 코드 */
    private final String stockCode;

    /** 종목명 */
    private final String stockName;

    /** 시장 종류 */
    private final MarketType marketType;

    /** 지수업종 대분류 코드 */
    private final String idxBztpLclsCode;

    /** 지수업종 대분류명 */
    private final String idxBztpLclsName;

    /** 지수업종 중분류 코드 */
    private final String idxBztpMclsCode;

    /** 지수업종 중분류명 */
    private final String idxBztpMclsName;

    /** 지수업종 소분류 코드 */
    private final String idxBztpSclsCode;

    /** 지수업종 소분류명 */
    private final String idxBztpSclsName;

    /** 표준산업분류 코드 (std_idst_clsf_cd) */
    private final String stdIdstClsfCode;

    /** 표준산업분류명 (std_idst_clsf_cd_name) */
    private final String stdIdstClsfName;

    /** 상장일 */
    private final LocalDate listedDate;

    /** 상장폐지일 */
    private final LocalDate delistedDate;

    /** 현재 상장 여부 */
    private final Boolean isActive;

    /** 마지막 갱신 시각 */
    private final LocalDateTime updatedAt;
}