package com.investory.ledger.domain.services.dto.result;

public record SecurityDetailResult(
    Long securityId,
    String securityCode,
    String securityName,
    String marketType,
    String sectorName
) {
}
