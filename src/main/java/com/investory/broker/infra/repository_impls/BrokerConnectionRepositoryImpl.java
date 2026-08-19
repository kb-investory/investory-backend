package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.infra.entities.BrokerConnectionRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.BrokerConnectionMapper;
import com.investory.core.exception.ErrorType;
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
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 연결 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<BrokerConnection> findActiveByUserIdAndBrokerId(Long userId, Long brokerId) {
        try {
            return brokerConnectionMapper.findActiveByUserIdAndBrokerId(userId, brokerId).stream()
                    .map(BrokerConnectionRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "활성 증권사 연결을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<BrokerConnection> findByIdAndUserId(Long connectionId, Long userId) {
        try {
            return brokerConnectionMapper.findByIdAndUserId(connectionId, userId).stream()
                    .map(BrokerConnectionRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 연결을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<BrokerConnection> findByIds(List<Long> connectionIds) {
        if (connectionIds.isEmpty()) {
            return List.of();
        }
        try {
            return brokerConnectionMapper.findByIds(connectionIds).stream()
                    .map(BrokerConnectionRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "connectionId 목록으로 증권사 연결을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<String> findMockProfileCodeByConnectionId(Long connectionId) {
        try {
            return brokerConnectionMapper.findMockProfileCodeByConnectionId(connectionId).stream().findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 연결의 인증 정보를 조회하는 중 오류가 발생했습니다.", e);
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
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 연결을 생성하는 중 오류가 발생했습니다.", e);
        }
        return row.getConnectionId();
    }

    @Override
    public void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt) {
        try {
            brokerConnectionMapper.updateLastSyncedAt(connectionId, lastSyncedAt);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 연결의 최근 동기화 시각을 갱신하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void updateStatus(Long connectionId, ConnectionStatus status) {
        try {
            brokerConnectionMapper.updateStatus(connectionId, status.name());
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "증권사 연결 상태를 변경하는 중 오류가 발생했습니다.", e);
        }
    }
}