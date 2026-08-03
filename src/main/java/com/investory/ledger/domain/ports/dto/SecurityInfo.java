package com.investory.ledger.domain.ports.dto;

public record SecurityInfo(
    Long securityId,
    String securityCode,
    String securityName,
    String marketType,
    String sectorName
) {
}
