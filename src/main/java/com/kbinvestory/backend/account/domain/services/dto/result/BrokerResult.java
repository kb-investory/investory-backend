package com.kbinvestory.backend.account.domain.services.dto.result;

import com.kbinvestory.backend.account.domain.model.BrokerageProvider;

public record BrokerResult(
    Long id,
    String code,
    String name,
    boolean active
) {
    public static BrokerResult from(BrokerageProvider provider) {
        return new BrokerResult(provider.getId(), provider.getCode(), provider.getName(), provider.isActive());
    }
}
