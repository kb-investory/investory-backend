package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.GainResponseType;
import com.investory.tendency.domain.services.dto.result.GainResponseAnalysisResult;

public record GainResponseAnalysisResponse(
    Long securityId,
    int totalGainDays,
    int netSellDays,
    int netBuyDays,
    int holdDays,
    GainResponseType type
) {
    public static GainResponseAnalysisResponse from(GainResponseAnalysisResult result) {
        return new GainResponseAnalysisResponse(
                result.securityId(),
                result.totalGainDays(),
                result.netSellDays(),
                result.netBuyDays(),
                result.holdDays(),
                result.type()
        );
    }
}
