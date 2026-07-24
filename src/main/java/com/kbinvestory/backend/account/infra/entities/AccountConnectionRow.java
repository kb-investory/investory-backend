package com.kbinvestory.backend.account.infra.entities;

import com.kbinvestory.backend.account.domain.constant.ConnectionStatus;
import com.kbinvestory.backend.account.domain.model.AccountConnection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AccountConnectionRow {
    private Long connectionId;
    private Long userId;
    private Long providerId;
    private String externalConnectionKey;
    private String connectionStatus;
    private Instant connectedAt;
    private Instant lastSyncedAt;
    private Instant disconnectedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public AccountConnection toDomain() {
        return AccountConnection.of(connectionId, userId, providerId, externalConnectionKey,
                ConnectionStatus.valueOf(connectionStatus), connectedAt, lastSyncedAt, disconnectedAt, createdAt, updatedAt);
    }

    public static AccountConnectionRow from(AccountConnection connection) {
        AccountConnectionRow row = new AccountConnectionRow();
        row.connectionId = connection.getConnectionId();
        row.userId = connection.getUserId();
        row.providerId = connection.getProviderId();
        row.externalConnectionKey = connection.getExternalConnectionKey();
        row.connectionStatus = connection.getConnectionStatus().name();
        row.connectedAt = connection.getConnectedAt();
        row.lastSyncedAt = connection.getLastSyncedAt();
        row.disconnectedAt = connection.getDisconnectedAt();
        row.createdAt = connection.getCreatedAt();
        row.updatedAt = connection.getUpdatedAt();
        return row;
    }
}