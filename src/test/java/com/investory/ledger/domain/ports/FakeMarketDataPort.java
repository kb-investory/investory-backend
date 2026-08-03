package com.investory.ledger.domain.ports;

import com.investory.ledger.domain.ports.dto.SecurityInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeMarketDataPort implements MarketDataPort {

    private final List<SecurityInfo> securities = new ArrayList<>();

    public void add(SecurityInfo... securities) {
        this.securities.addAll(List.of(securities));
    }

    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return securities.stream()
                .filter(security -> securityIds.contains(security.securityId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SecurityInfo> resolveByCode(String securityCode) {
        return securities.stream()
                .filter(security -> security.securityCode().equals(securityCode))
                .findFirst();
    }
}
