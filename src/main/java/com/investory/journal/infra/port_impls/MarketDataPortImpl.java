package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.ports.MarketDataPort;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import org.springframework.stereotype.Component;

import java.util.List;

// 빈 이름 명시: ledger.infra.port_impls.MarketDataPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(marketDataPortImpl)이 충돌한다.
@Component("journalMarketDataPortImpl")
public class MarketDataPortImpl implements MarketDataPort {

    // TODO: market.securities 구현 후 실제 조회로 교체. market이 아직 없어 항상 빈 리스트를 반환한다.
    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return List.of();
    }
}
