package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerConnection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BrokerConnectionRepository {
    List<BrokerConnection> findAllByUserId(Long userId);

    Optional<BrokerConnection> findActiveByUserIdAndBrokerId(Long userId, Long brokerId);

    Optional<BrokerConnection> findByIdAndUserId(Long connectionId, Long userId);

    // 소유권 확인 없이 표시 정보 조립용으로만 사용 (예: 다른 도메인의 Port에 계좌 목록을 넘겨줄 때)
    List<BrokerConnection> findByIds(List<Long> connectionIds);

    Long insert(Long userId, Long brokerId, String mockProfileCode, Instant connectedAt);

    void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt);
}