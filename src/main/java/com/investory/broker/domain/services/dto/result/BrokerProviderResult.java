package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.model.BrokerProvider;

public record BrokerProviderResult(
    Long brokerId,
    String brokerCode,
    String brokerName
) {
    public static BrokerProviderResult from(BrokerProvider provider) {
        return new BrokerProviderResult(provider.getBrokerId(), provider.getBrokerCode(), provider.getBrokerName());
    }
}