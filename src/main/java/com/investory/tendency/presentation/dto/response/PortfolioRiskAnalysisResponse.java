package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.ConcentrationLevel;
import com.investory.tendency.domain.constant.PortfolioRiskType;
import com.investory.tendency.domain.constant.VolatilityLevel;
import com.investory.tendency.domain.services.dto.result.PortfolioRiskAnalysisResult;

import java.math.BigDecimal;

public record PortfolioRiskAnalysisResponse(
    BigDecimal maxSecurityWeight,
    ConcentrationLevel concentration,
    BigDecimal weightedVolatility,
    VolatilityLevel volatility,
    PortfolioRiskType type
) {
    public static PortfolioRiskAnalysisResponse from(PortfolioRiskAnalysisResult result) {
        return new PortfolioRiskAnalysisResponse(
                result.maxSecurityWeight(),
                result.concentration(),
                result.weightedVolatility(),
                result.volatility(),
                result.type()
        );
    }
}
