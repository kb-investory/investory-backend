package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.ConcentrationLevel;
import com.investory.tendency.domain.constant.PortfolioRiskType;
import com.investory.tendency.domain.constant.VolatilityLevel;

import java.math.BigDecimal;

public record PortfolioRiskAnalysisResult(
    BigDecimal maxSecurityWeight,     // 최대 종목 비중(%)
    ConcentrationLevel concentration,
    BigDecimal weightedVolatility,    // 가중평균 변동성(%, 일간등락률 표준편차 기준)
    VolatilityLevel volatility,
    PortfolioRiskType type
) {
}
