package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<BrokerProvider> findById(Long brokerId) {
        return providers.stream()
                .filter(provider -> provider.getBrokerId().equals(brokerId))
                .findFirst();
    }

    @Override
    public void upsertByCode(String brokerCode, String brokerName) {
        Optional<BrokerProvider> existing = providers.stream()
                .filter(provider -> provider.getBrokerCode().equals(brokerCode))
                .findFirst();
        if (existing.isPresent()) {
            BrokerProvider updated = BrokerProvider.of(existing.get().getBrokerId(), brokerCode, brokerName, true);
            providers.remove(existing.get());
            providers.add(updated);
        } else {
            long nextId = providers.stream().mapToLong(BrokerProvider::getBrokerId).max().orElse(0L) + 1;
            providers.add(BrokerProvider.of(nextId, brokerCode, brokerName, true));
        }
    }

    @Override
    public void deactivateExcept(List<String> brokerCodes) {
        List<BrokerProvider> updated = providers.stream()
                .map(provider -> brokerCodes.contains(provider.getBrokerCode())
                        ? provider
                        : BrokerProvider.of(provider.getBrokerId(), provider.getBrokerCode(), provider.getBrokerName(), false))
                .collect(Collectors.toList());
        providers.clear();
        providers.addAll(updated);
    }
}