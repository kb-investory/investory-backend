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
        AccountConnection saved = accountConnection.getConnectionId() == null
                ? AccountConnection.of(sequence++, accountConnection.getUserId(), accountConnection.getProviderId(),
                        accountConnection.getExternalConnectionKey(), accountConnection.getConnectionStatus(),
                        accountConnection.getConnectedAt(), accountConnection.getLastSyncedAt(),
                        accountConnection.getDisconnectedAt(), accountConnection.getCreatedAt(), accountConnection.getUpdatedAt())
                : accountConnection;
        connections.add(saved);
        return saved;
    }
}
