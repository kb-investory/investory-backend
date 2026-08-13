package com.investory.tendency.infra.port_impls;

import com.investory.ledger.domain.services.HoldingQueryService;
import com.investory.ledger.domain.services.dto.query.GetHoldingsQuery;
import com.investory.tendency.domain.ports.HoldingSummaryPort;
import com.investory.tendency.domain.ports.dto.HoldingWeightInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 빈 이름 명시: broker.infra.port_impls.HoldingSummaryPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(holdingSummaryPortImpl)이 충돌한다.
@Component("tendencyHoldingSummaryPortImpl")
public class HoldingSummaryPortImpl implements HoldingSummaryPort {

    private final HoldingQueryService holdingQueryService;

    public HoldingSummaryPortImpl(HoldingQueryService holdingQueryService) {
        this.holdingQueryService = holdingQueryService;
    }

    @Override
    public List<HoldingWeightInfo> findHoldingWeights(Long userId) {
        return holdingQueryService.getHoldings(new GetHoldingsQuery(userId, null, null))
                .holdings().stream()
                .map(h -> new HoldingWeightInfo(h.securityId(), h.portfolioWeight()))
                .collect(Collectors.toList());
    }
}
