package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.GainResponseType;

public record GainResponseAnalysisResult(
    Long securityId,
    int totalGainDays,
    int netSellDays,
    int netBuyDays,
    int holdDays,
    GainResponseType type
) {
}
