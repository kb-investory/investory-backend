package com.investory.broker.infra.entities;

import com.investory.broker.domain.model.BrokerProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BrokerProviderRow {
    private Long brokerId;
    private String brokerCode;
    private String brokerName;
    private boolean isActive;

    public BrokerProvider toDomain() {
        return BrokerProvider.of(brokerId, brokerCode, brokerName, isActive);
    }
}