package com.kbinvestory.backend.account.presentation.dto.response;

import com.kbinvestory.backend.account.domain.constant.ConnectionStatus;
import com.kbinvestory.backend.account.domain.services.dto.result.BrokerConnectionResult;

public record BrokerConnectionResponse(
    Long connectionId,
    ConnectionStatus status
) {
    public static BrokerConnectionResponse from(BrokerConnectionResult result) {
        return new BrokerConnectionResponse(result.connectionId(), result.status());
    }
}
