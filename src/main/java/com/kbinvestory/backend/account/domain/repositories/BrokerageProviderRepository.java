package com.kbinvestory.backend.account.domain.repositories;

import com.kbinvestory.backend.account.domain.model.BrokerageProvider;
import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;

import java.util.List;
import java.util.Optional;

public interface BrokerageProviderRepository {
    List<BrokerageProvider> search(GetBrokersQuery query);
    Optional<BrokerageProvider> findById(Long providerId);
}
