package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.constant.MarketType;
import com.investory.journal.domain.ports.MarketDataPort;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import com.investory.market.domain.services.SecurityLookupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 빈 이름 명시: ledger.infra.port_impls.MarketDataPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(marketDataPortImpl)이 충돌한다.
@Component("journalMarketDataPortImpl")
public class MarketDataPortImpl implements MarketDataPort {

    private final SecurityLookupService securityLookupService;

    public MarketDataPortImpl(SecurityLookupService securityLookupService) {
        this.securityLookupService = securityLookupService;
    }

    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return securityLookupService.findByIds(securityIds).stream()
                .map(security -> new SecurityInfo(
                        security.getSecurityId(),
                        security.getSecurityCode(),
                        security.getSecurityName(),
                        MarketType.valueOf(security.getMarketType().name())
                ))
                .collect(Collectors.toList());
    }
}
