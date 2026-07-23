package com.kbinvestory.backend.account.domain.model;

import lombok.Getter;

import java.time.Instant;

@Getter
public class BrokerageProvider {

    private final Long id;
    private final String code;
    private final String name;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private BrokerageProvider(Long id, String code, String name, boolean active,
                               Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static BrokerageProvider of(Long id, String code, String name, boolean active,
                                        Instant createdAt, Instant updatedAt) {
        return new BrokerageProvider(id, code, name, active, createdAt, updatedAt);
    }
}
