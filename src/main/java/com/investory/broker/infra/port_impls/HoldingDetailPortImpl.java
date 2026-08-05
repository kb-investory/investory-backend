package com.investory.broker.infra.port_impls;

import com.investory.broker.domain.ports.HoldingDetailPort;
import com.investory.broker.domain.ports.dto.AccountHoldingsInfo;
import com.investory.broker.domain.ports.dto.HoldingDetailInfo;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;
import com.investory.ledger.domain.services.HoldingQueryService;
import com.investory.ledger.domain.services.dto.query.GetHoldingsQuery;
import com.investory.ledger.domain.services.dto.result.HoldingListResult;
import com.investory.ledger.domain.services.dto.result.HoldingResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HoldingDetailPortImpl implements HoldingDetailPort {

    private final HoldingQueryService holdingQueryService;

    public HoldingDetailPortImpl(HoldingQueryService holdingQueryService) {
        this.holdingQueryService = holdingQueryService;
    }

    @Override
    public AccountHoldingsInfo getHoldings(Long userId, Long accountId) {
        HoldingListResult result = holdingQueryService.getHoldings(new GetHoldingsQuery(userId, accountId, null));

        HoldingSummaryInfo summary = new HoldingSummaryInfo(
                result.summary().holdingCount(), result.summary().totalMarketValue(), result.summary().totalProfitLossAmount());
        List<HoldingDetailInfo> holdings = result.holdings().stream()
                .map(holding -> toHoldingDetailInfo(holding, result.snapshotDate()))
                .collect(Collectors.toList());

        return new AccountHoldingsInfo(summary, holdings);
    }

    private HoldingDetailInfo toHoldingDetailInfo(HoldingResult holding, LocalDate snapshotDate) {
        return new HoldingDetailInfo(
                holding.securityId(),
                holding.securityCode(),
                holding.securityName(),
                holding.marketType(),
                holding.quantity(),
                holding.averagePurchasePrice(),
                holding.marketValue(),
                holding.profitLossAmount(),
                holding.portfolioWeight(),
                snapshotDate
        );
    }
}
