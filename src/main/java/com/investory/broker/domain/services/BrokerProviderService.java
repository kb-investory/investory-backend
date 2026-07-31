package com.investory.broker.domain.services;

import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.domain.services.dto.result.BrokerProviderResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerProviderService {

    private final BrokerProviderRepository brokerProviderRepository;

    public BrokerProviderService(BrokerProviderRepository brokerProviderRepository) {
        this.brokerProviderRepository = brokerProviderRepository;
    }

    public List<BrokerProviderResult> getProviders() {
        return brokerProviderRepository.findAllActive().stream()
                .map(BrokerProviderResult::from)
                .collect(Collectors.toList());
    }
}