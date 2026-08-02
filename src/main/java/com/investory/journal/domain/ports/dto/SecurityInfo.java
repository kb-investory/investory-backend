package com.investory.journal.domain.ports.dto;

import com.investory.journal.domain.constant.MarketType;

public record SecurityInfo(
    Long securityId,
    String securityCode,
    String securityName,
    MarketType marketType
) {
}
