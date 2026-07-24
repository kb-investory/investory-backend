package com.kbinvestory.backend.account.domain.repositories;

import com.kbinvestory.backend.account.domain.model.AccountConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FakeAccountConnectionRepository implements AccountConnectionRepository {

    private final List<AccountConnection> connections = new ArrayList<>();
    private long sequence = 1;

    public void add(AccountConnection... connections) {
        this.connections.addAll(List.of(connections));
    }

    @Override
    public Optional<AccountConnection> findByUserIdAndProviderId(Long userId, Long providerId) {
        return connections.stream()
                .filter(connection -> Objects.equals(connection.getUserId(), userId)
                        && Objects.equals(connection.getProviderId(), providerId))
                .findFirst();
    }

    @Override
    public AccountConnection save(AccountConnection accountConnection) {
        // 실제 DB의 upsert(user_id, provider_id 유니크 키 기준)를 흉내냄
        Optional<AccountConnection> existing = findByUserIdAndProviderId(
                accountConnection.getUserId(), accountConnection.getProviderId());
        Long connectionId = existing.map(AccountConnection::getConnectionId).orElseGet(() -> sequence++);

        AccountConnection saved = AccountConnection.of(connectionId, accountConnection.getUserId(), accountConnection.getProviderId(),
                accountConnection.getExternalConnectionKey(), accountConnection.getConnectionStatus(),
                accountConnection.getConnectedAt(), accountConnection.getLastSyncedAt(),
                accountConnection.getDisconnectedAt(), accountConnection.getCreatedAt(), accountConnection.getUpdatedAt());

        existing.ifPresent(connections::remove);
        connections.add(saved);
        return saved;
    }
}
