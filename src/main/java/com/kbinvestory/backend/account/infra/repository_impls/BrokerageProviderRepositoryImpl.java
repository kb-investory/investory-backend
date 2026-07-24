package com.kbinvestory.backend.account.infra.repository_impls;

import com.kbinvestory.backend.account.domain.model.BrokerageProvider;
import com.kbinvestory.backend.account.domain.repositories.BrokerageProviderRepository;
import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;
import com.kbinvestory.backend.account.infra.entities.BrokerageProviderRow;
import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;
import com.kbinvestory.backend.account.infra.mappers.BrokerageProviderMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BrokerageProviderRepositoryImpl implements BrokerageProviderRepository {

    private final BrokerageProviderMapper brokerageProviderMapper;

    public BrokerageProviderRepositoryImpl(BrokerageProviderMapper brokerageProviderMapper) {
        this.brokerageProviderMapper = brokerageProviderMapper;
    }

    @Override
    public List<BrokerageProvider> search(GetBrokersQuery query) {
        try {
            return brokerageProviderMapper.search(query).stream()
                    .map(BrokerageProviderRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new AccountInfraException(AccountInfraErrorCode.BROKERAGE_PROVIDER_QUERY_FAILED, e);
        }
    }

    @Override
    public Optional<BrokerageProvider> findById(Long providerId) {
        try {
            return Optional.ofNullable(brokerageProviderMapper.findById(providerId))
                    .map(BrokerageProviderRow::toDomain);
        } catch (DataAccessException e) {
            throw new AccountInfraException(AccountInfraErrorCode.BROKERAGE_PROVIDER_QUERY_FAILED, e);
        }
    }
}