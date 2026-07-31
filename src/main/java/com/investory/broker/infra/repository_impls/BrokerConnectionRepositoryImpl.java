package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.infra.entities.BrokerConnectionRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerConnectionMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BrokerConnectionRepositoryImpl implements BrokerConnectionRepository {

    private final BrokerConnectionMapper brokerConnectionMapper;

    public BrokerConnectionRepositoryImpl(BrokerConnectionMapper brokerConnectionMapper) {
        this.brokerConnectionMapper = brokerConnectionMapper;
    }

    @Override
    public List<BrokerConnection> findAllByUserId(Long userId) {
        try {
            return brokerConnectionMapper.findAllByUserId(userId).stream()
                    .map(BrokerConnectionRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }
}