package com.kbinvestory.backend.account.domain.model;

import com.kbinvestory.backend.account.domain.constant.ConnectionStatus;
import com.kbinvestory.backend.account.domain.exception.AccountConnectionException;
import com.kbinvestory.backend.account.domain.exception.AccountErrorCode;
import lombok.Getter;

import java.time.Instant;

@Getter
public class AccountConnection {

    private final Long connectionId;
    private final Long userId;
    private final Long providerId;
    private final String externalConnectionKey;
    private final ConnectionStatus connectionStatus;
    private final Instant connectedAt;
    private final Instant lastSyncedAt;
    private final Instant disconnectedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private AccountConnection(Long connectionId, Long userId, Long providerId, String externalConnectionKey,
                               ConnectionStatus connectionStatus, Instant connectedAt, Instant lastSyncedAt,
                               Instant disconnectedAt, Instant createdAt, Instant updatedAt) {
        requireNonNull(userId);
        requireNonNull(providerId);
        requireNonNull(connectionStatus);
        requireNonNull(connectedAt);

        this.connectionId = connectionId;
        this.userId = userId;
        this.providerId = providerId;
        this.externalConnectionKey = externalConnectionKey;
        this.connectionStatus = connectionStatus;
        this.connectedAt = connectedAt;
        this.lastSyncedAt = lastSyncedAt;
        this.disconnectedAt = disconnectedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new AccountConnectionException(AccountErrorCode.INVALID_CONNECTION_DATA);
        }
    }

    // 신규 연동: CODEF 인증 성공 후 발급받은 connectedId로 생성
    public static AccountConnection create(Long userId, Long providerId, String externalConnectionKey) {
        Instant now = Instant.now();
        return new AccountConnection(null, userId, providerId, externalConnectionKey,
                ConnectionStatus.CONNECTED, now, null, null, now, now);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static AccountConnection of(Long connectionId, Long userId, Long providerId, String externalConnectionKey,
                                        ConnectionStatus connectionStatus, Instant connectedAt, Instant lastSyncedAt,
                                        Instant disconnectedAt, Instant createdAt, Instant updatedAt) {
        return new AccountConnection(connectionId, userId, providerId, externalConnectionKey,
                connectionStatus, connectedAt, lastSyncedAt, disconnectedAt, createdAt, updatedAt);
    }
}
