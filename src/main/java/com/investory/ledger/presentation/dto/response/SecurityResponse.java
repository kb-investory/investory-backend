package com.investory.ledger.presentation.dto.response;

import com.investory.ledger.domain.services.dto.result.SecurityDetailResult;

public record SecurityResponse(
    Long securityId,
    String securityCode,
    String securityName,
    String marketType,
    String sectorName
) {
    public static SecurityResponse from(SecurityDetailResult result) {
        return new SecurityResponse(result.securityId(), result.securityCode(), result.securityName(),
                result.marketType(), result.sectorName());
    }
}
