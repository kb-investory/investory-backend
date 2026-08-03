package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerProvider;

import java.util.List;
import java.util.Optional;

public interface BrokerProviderRepository {
    List<BrokerProvider> findAllActive();

    Optional<BrokerProvider> findById(Long brokerId);
}