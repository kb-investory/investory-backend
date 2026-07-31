package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeBrokerProviderRepository implements BrokerProviderRepository {

    private final List<BrokerProvider> providers = new ArrayList<>();

    public void add(BrokerProvider... providers) {
        this.providers.addAll(List.of(providers));
    }

    @Override
    public List<BrokerProvider> findAllActive() {
        return providers.stream()
                .filter(BrokerProvider::isActive)
                .collect(Collectors.toList());
    }
}