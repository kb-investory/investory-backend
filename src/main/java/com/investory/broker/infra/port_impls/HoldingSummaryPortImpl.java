package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.HoldingSummaryPort;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;
import com.investory.ledger.domain.services.HoldingQueryService;
import com.investory.ledger.domain.services.dto.query.GetHoldingsQuery;
import com.investory.ledger.domain.services.dto.result.HoldingListResult;
import com.investory.ledger.domain.services.dto.result.HoldingSummaryResult;
import org.springframework.stereotype.Component;

@Component
public class HoldingSummaryPortImpl implements HoldingSummaryPort {

    private final HoldingQueryService holdingQueryService;

    public HoldingSummaryPortImpl(HoldingQueryService holdingQueryService) {
        this.holdingQueryService = holdingQueryService;
    }

    @Override
    public HoldingSummaryInfo summarize(Long userId, Long accountId) {
        HoldingListResult result = holdingQueryService.getHoldings(new GetHoldingsQuery(userId, accountId, null));
        HoldingSummaryResult summary = result.summary();
        return new HoldingSummaryInfo(summary.holdingCount(), summary.totalMarketValue(), summary.totalProfitLossAmount());
    }
}
