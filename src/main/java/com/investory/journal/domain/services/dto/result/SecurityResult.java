package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.MarketType;
import com.investory.journal.domain.ports.dto.SecurityInfo;

public record SecurityResult(
    Long securityId,
    String securityCode,
    String securityName,
    MarketType marketType
) {
    public static SecurityResult from(SecurityInfo info) {
        return new SecurityResult(info.securityId(), info.securityCode(), info.securityName(), info.marketType());
    }
}
