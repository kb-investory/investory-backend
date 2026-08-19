package com.investory.ledger.infra.port_impls;

import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.market.domain.model.Security;
import com.investory.market.domain.services.SecurityLookupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// 빈 이름 명시: journal.infra.port_impls.MarketDataPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(marketDataPortImpl)이 충돌한다.
@Component("ledgerMarketDataPortImpl")
public class MarketDataPortImpl implements MarketDataPort {

    private final SecurityLookupService securityLookupService;

    public MarketDataPortImpl(SecurityLookupService securityLookupService) {
        this.securityLookupService = securityLookupService;
    }

    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return securityLookupService.findByIds(securityIds).stream()
                .map(this::toSecurityInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SecurityInfo> resolveByCode(String securityCode) {
        return securityLookupService.findByCode(securityCode).map(this::toSecurityInfo);
    }

    @Override
    public Map<String, SecurityInfo> resolveByCodes(List<String> securityCodes) {
        if (securityCodes.isEmpty()) {
            return Map.of();
        }
        return securityLookupService.findByCodes(securityCodes).stream()
                .map(this::toSecurityInfo)
                .collect(Collectors.toMap(SecurityInfo::securityCode, Function.identity()));
    }

    private SecurityInfo toSecurityInfo(Security security) {
        return new SecurityInfo(
                security.getSecurityId(),
                security.getSecurityCode(),
                security.getSecurityName(),
                security.getMarketType().name(),
                security.getSectorName()
        );
    }
}
