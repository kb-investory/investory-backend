package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerProvider;

import java.util.List;

public interface BrokerProviderRepository {
    List<BrokerProvider> findAllActive();
}