package com.investory.ledger.infra.port_impls;

import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MarketDataPortImpl implements MarketDataPort {

    // TODO: market.securities 구현 후 실제 조회로 교체. market이 아직 없어 항상 빈 값을 반환한다.
    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return List.of();
    }

    // TODO: market.securities 구현 후 실제 조회로 교체. market이 아직 없어 항상 빈 값을 반환한다.
    @Override
    public Optional<SecurityInfo> resolveByCode(String securityCode) {
        return Optional.empty();
    }
}
