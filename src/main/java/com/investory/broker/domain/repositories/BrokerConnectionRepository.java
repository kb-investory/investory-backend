package com.investory.broker.domain.repositories;

import com.investory.broker.domain.model.BrokerConnection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BrokerConnectionRepository {
    List<BrokerConnection> findAllByUserId(Long userId);

    Optional<BrokerConnection> findActiveByUserIdAndBrokerId(Long userId, Long brokerId);

    Optional<BrokerConnection> findByIdAndUserId(Long connectionId, Long userId);

    Long insert(Long userId, Long brokerId, String mockProfileCode, Instant connectedAt);

    void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt);
}