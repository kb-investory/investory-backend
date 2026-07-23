package com.kbinvestory.backend.account.domain.repositories;

import com.kbinvestory.backend.account.domain.model.BrokerageProvider;
import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;

import java.util.List;

public interface BrokerageProviderRepository {
    List<BrokerageProvider> search(GetBrokersQuery query);
}
