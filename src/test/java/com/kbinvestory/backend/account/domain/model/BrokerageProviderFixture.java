package com.kbinvestory.backend.account.domain.model;

import java.time.Instant;

public class BrokerageProviderFixture {

    public static BrokerageProvider provider(String code, String name, boolean active) {
        return BrokerageProvider.of(null, code, name, active, Instant.now(), Instant.now());
    }
}