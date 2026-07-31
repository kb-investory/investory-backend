package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.services.dto.result.BrokerProviderResult;

public record BrokerProviderResponse(
    Long brokerId,
    String brokerCode,
    String brokerName
) {
    public static BrokerProviderResponse from(BrokerProviderResult result) {
        return new BrokerProviderResponse(result.brokerId(), result.brokerCode(), result.brokerName());
    }
}