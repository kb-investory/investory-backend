package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.model.BrokerProvider;
import com.investory.broker.domain.repositories.BrokerProviderRepository;
import com.investory.broker.infra.entities.BrokerProviderRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerProviderMapper;
import com.investory.core.exception.ErrorType;
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
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<BrokerProvider> findById(Long brokerId) {
        try {
            return brokerProviderMapper.findById(brokerId).stream()
                    .map(BrokerProviderRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 정보를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void upsertByCode(String brokerCode, String brokerName) {
        try {
            boolean exists = !brokerProviderMapper.findByCode(brokerCode).isEmpty();
            if (exists) {
                brokerProviderMapper.updateByCode(brokerCode, brokerName);
            } else {
                BrokerProviderRow row = new BrokerProviderRow();
                row.setBrokerCode(brokerCode);
                row.setBrokerName(brokerName);
                row.setActive(true);
                brokerProviderMapper.insert(row);
            }
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 정보를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deactivateExcept(List<String> brokerCodes) {
        try {
            brokerProviderMapper.deactivateExcept(brokerCodes);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 목록을 갱신하는 중 오류가 발생했습니다.", e);
        }
    }
}