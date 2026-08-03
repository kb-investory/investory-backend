package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeBrokerConnectionRepository implements BrokerConnectionRepository {

    private record Owned(Long userId, BrokerConnection connection) {
    }

    private final List<Owned> connections = new ArrayList<>();
    private long nextConnectionId = 100L;

    public void add(Long userId, BrokerConnection connection) {
        connections.add(new Owned(userId, connection));
    }

    @Override
    public List<BrokerConnection> findAllByUserId(Long userId) {
        return connections.stream()
                .filter(owned -> owned.userId().equals(userId))
                .map(Owned::connection)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BrokerConnection> findActiveByUserIdAndBrokerId(Long userId, Long brokerId) {
        return connections.stream()
                .filter(owned -> owned.userId().equals(userId))
                .map(Owned::connection)
                .filter(connection -> connection.getBrokerId().equals(brokerId))
                .filter(connection -> connection.getConnectionStatus() == ConnectionStatus.CONNECTED)
                .findFirst();
    }

    @Override
    public Optional<BrokerConnection> findByIdAndUserId(Long connectionId, Long userId) {
        return connections.stream()
                .filter(owned -> owned.userId().equals(userId))
                .map(Owned::connection)
                .filter(connection -> connection.getConnectionId().equals(connectionId))
                .findFirst();
    }

    @Override
    public Long insert(Long userId, Long brokerId, String mockProfileCode, Instant connectedAt) {
        Long connectionId = nextConnectionId++;
        BrokerConnection connection = BrokerConnection.of(
                connectionId, brokerId, null, null,
                ConnectionStatus.CONNECTED, connectedAt, null, 0
        );
        connections.add(new Owned(userId, connection));
        return connectionId;
    }

    @Override
    public void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt) {
        // 테스트에서 이 값을 직접 검증할 필요가 없어 no-op으로 둔다.
    }
}