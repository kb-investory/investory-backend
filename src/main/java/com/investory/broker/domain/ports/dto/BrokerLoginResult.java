package com.investory.broker.domain.ports.dto;

public record BrokerLoginResult(
    String accessToken,
    String orgCode,
    String orgName
) {
}
