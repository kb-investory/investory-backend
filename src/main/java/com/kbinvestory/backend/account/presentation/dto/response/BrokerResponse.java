package com.kbinvestory.backend.account.presentation.dto.response;

import com.kbinvestory.backend.account.domain.services.dto.result.BrokerResult;

public record BrokerResponse(
    Long providerId,
    String providerCode,
    String providerName,
    boolean active
) {
    public static BrokerResponse from(BrokerResult result) {
        return new BrokerResponse(result.id(), result.code(), result.name(), result.active());
    }
}
