package com.kbinvestory.backend.account.infra.entities;

import com.kbinvestory.backend.account.domain.model.BrokerageProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class BrokerageProviderRow {
    private Long providerId;
    private String providerCode;
    private String providerName;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public BrokerageProvider toDomain() {
        return BrokerageProvider.of(providerId, providerCode, providerName, active, createdAt, updatedAt);
    }
}