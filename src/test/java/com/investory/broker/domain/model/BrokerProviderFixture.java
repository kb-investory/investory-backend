package com.investory.broker.domain.model;

public class BrokerProviderFixture {

    public static BrokerProvider provider(Long brokerId, String brokerCode, String brokerName) {
        return provider(brokerId, brokerCode, brokerName, true);
    }

    public static BrokerProvider provider(Long brokerId, String brokerCode, String brokerName, boolean active) {
        return BrokerProvider.of(brokerId, brokerCode, brokerName, active);
    }
}