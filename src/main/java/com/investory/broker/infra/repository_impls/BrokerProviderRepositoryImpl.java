package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.model.BrokerProvider;
import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.infra.entities.BrokerProviderRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerProviderMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BrokerProviderRepositoryImpl implements BrokerProviderRepository {

    private final BrokerProviderMapper brokerProviderMapper;

    public BrokerProviderRepositoryImpl(BrokerProviderMapper brokerProviderMapper) {
        this.brokerProviderMapper = brokerProviderMapper;
    }

    @Override
    public List<BrokerProvider> findAllActive() {
        try {
            return brokerProviderMapper.findAllActive().stream()
                    .map(BrokerProviderRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public Optional<BrokerProvider> findById(Long brokerId) {
        try {
            return brokerProviderMapper.findById(brokerId).stream()
                    .map(BrokerProviderRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }
}