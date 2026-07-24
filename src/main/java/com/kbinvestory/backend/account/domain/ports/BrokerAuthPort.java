package com.kbinvestory.backend.account.domain.ports;

import com.kbinvestory.backend.account.domain.ports.dto.BrokerAuthInfo;

public interface BrokerAuthPort {
    BrokerAuthInfo authenticate(String providerCode, String loginId, String password);
}
