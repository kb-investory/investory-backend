package com.investory.journal.domain.ports.dto;

public record SecurityInfo(
    Long securityId,
    String securityCode,
    String securityName
) {
}
