package com.investory.market.domain.ports.dto;

import com.investory.market.domain.constant.MarketType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StockInfoDto {

    /** 종목 코드 */
    private final String stockCode;

    /** 종목명 */
    private final String stockName;

    /** 시장 종류 */
    private final MarketType marketType;

    /** 표준산업분류 코드 (industry_code) */
    private final String stdIdstClsfCode;

    /** 표준산업분류명 (industry_name) */
    private final String stdIdstClsfName;

    /** 상장일 */
    private final LocalDate listedDate;

    /** 상장폐지일 */
    private final LocalDate delistedDate;

    /** 현재 상장 여부 */
    private final Boolean isActive;
}
