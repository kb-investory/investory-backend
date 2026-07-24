package com.kbinvestory.backend.account.domain.services.dto.result;

import com.kbinvestory.backend.account.domain.constant.ConnectionStatus;
import com.kbinvestory.backend.account.domain.model.AccountConnection;

public record BrokerConnectionResult(
    Long connectionId,
    ConnectionStatus status
) {
    public static BrokerConnectionResult from(AccountConnection connection) {
        return new BrokerConnectionResult(connection.getConnectionId(), connection.getConnectionStatus());
    }
}