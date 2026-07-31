package com.investory.broker.domain.services;

import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.services.dto.result.BrokerConnectionResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrokerConnectionService {

    private final BrokerConnectionRepository brokerConnectionRepository;

    public BrokerConnectionService(BrokerConnectionRepository brokerConnectionRepository) {
        this.brokerConnectionRepository = brokerConnectionRepository;
    }

    public List<BrokerConnectionResult> getConnections(Long userId) {
        return brokerConnectionRepository.findAllByUserId(userId).stream()
                .map(BrokerConnectionResult::from)
                .collect(Collectors.toList());
    }
}