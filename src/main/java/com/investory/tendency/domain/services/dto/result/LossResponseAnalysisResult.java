package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.LossResponseType;

public record LossResponseAnalysisResult(
    Long securityId,
    int totalLossDays,
    int netSellDays,
    int netBuyDays,
    int holdDays,
    LossResponseType type
) {
}
