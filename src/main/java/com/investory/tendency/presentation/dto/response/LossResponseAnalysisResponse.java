package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.LossResponseType;
import com.investory.tendency.domain.services.dto.result.LossResponseAnalysisResult;

public record LossResponseAnalysisResponse(
    Long securityId,
    int totalLossDays,
    int netSellDays,
    int netBuyDays,
    int holdDays,
    LossResponseType type
) {
    public static LossResponseAnalysisResponse from(LossResponseAnalysisResult result) {
        return new LossResponseAnalysisResponse(
                result.securityId(),
                result.totalLossDays(),
                result.netSellDays(),
                result.netBuyDays(),
                result.holdDays(),
                result.type()
        );
    }
}
