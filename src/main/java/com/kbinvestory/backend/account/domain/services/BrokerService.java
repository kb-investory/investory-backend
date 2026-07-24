package com.kbinvestory.backend.account.domain.services;

import com.kbinvestory.backend.account.domain.repositories.BrokerageProviderRepository;
import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerService {

    private final BrokerageProviderRepository brokerageProviderRepository;

    public BrokerService(BrokerageProviderRepository brokerageProviderRepository) {
        this.brokerageProviderRepository = brokerageProviderRepository;
    }

    public List<BrokerResult> getBrokers(GetBrokersQuery query) {
        return brokerageProviderRepository.search(query).stream()
                .map(BrokerResult::from)
                .collect(Collectors.toList());
    }
}
