package com.kbinvestory.backend.account.domain.repositories;

import com.kbinvestory.backend.account.domain.model.BrokerageProvider;
import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeBrokerageProviderRepository implements BrokerageProviderRepository {

    private final List<BrokerageProvider> providers = new ArrayList<>();

    public void add(BrokerageProvider... providers) {
        this.providers.addAll(List.of(providers));
    }

    @Override
    public List<BrokerageProvider> search(GetBrokersQuery query) {
        return providers.stream()
                .filter(BrokerageProvider::isActive)
                .filter(provider -> matchesKeyword(provider, query.keyword()))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(BrokerageProvider provider, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return provider.getCode().contains(keyword) || provider.getName().contains(keyword);
    }
}