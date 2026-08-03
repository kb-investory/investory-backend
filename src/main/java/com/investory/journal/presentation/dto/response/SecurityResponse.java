package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.constant.MarketType;
import com.investory.journal.domain.services.dto.result.SecurityResult;

public record SecurityResponse(
        Long securityId,
        String securityCode,
        String securityName,
        MarketType marketType
) {
    public static SecurityResponse from(SecurityResult result) {
        return new SecurityResponse(result.securityId(), result.securityCode(), result.securityName(), result.marketType());
    }
}
