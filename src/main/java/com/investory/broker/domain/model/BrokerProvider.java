package com.investory.broker.domain.model;

import lombok.Getter;

@Getter
public class BrokerProvider {

    private final Long brokerId;
    private final String brokerCode;
    private final String brokerName;
    private final boolean isActive;

    private BrokerProvider(Long brokerId, String brokerCode, String brokerName, boolean isActive) {
        this.brokerId = brokerId;
        this.brokerCode = brokerCode;
        this.brokerName = brokerName;
        this.isActive = isActive;
    }

    public static BrokerProvider of(Long brokerId, String brokerCode, String brokerName, boolean isActive) {
        return new BrokerProvider(brokerId, brokerCode, brokerName, isActive);
    }
}