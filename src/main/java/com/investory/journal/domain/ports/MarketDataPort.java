package com.investory.journal.domain.ports;

import com.investory.journal.domain.ports.dto.SecurityInfo;

import java.util.List;

public interface MarketDataPort {
    List<SecurityInfo> findSecurities(List<Long> securityIds);
}
