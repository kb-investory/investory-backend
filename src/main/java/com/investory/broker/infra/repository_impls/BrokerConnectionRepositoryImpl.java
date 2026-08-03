package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.infra.entities.BrokerConnectionRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerConnectionMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<BrokerConnection> findActiveByUserIdAndBrokerId(Long userId, Long brokerId) {
        try {
            return brokerConnectionMapper.findActiveByUserIdAndBrokerId(userId, brokerId).stream()
                    .map(BrokerConnectionRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public Long insert(Long userId, Long brokerId, String mockProfileCode, Instant connectedAt) {
        BrokerConnectionRow row = new BrokerConnectionRow();
        row.setUserId(userId);
        row.setBrokerId(brokerId);
        row.setMockProfileCode(mockProfileCode);
        row.setConnectedAt(connectedAt);
        try {
            brokerConnectionMapper.insert(row);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
        return row.getConnectionId();
    }

    @Override
    public void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt) {
        try {
            brokerConnectionMapper.updateLastSyncedAt(connectionId, lastSyncedAt);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }
}