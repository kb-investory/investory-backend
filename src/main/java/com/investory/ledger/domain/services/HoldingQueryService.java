package com.investory.ledger.domain.services;

import com.investory.ledger.domain.exception.LedgerErrorCode;
import com.investory.ledger.domain.exception.LedgerException;
import com.investory.ledger.domain.model.Holding;
import com.investory.ledger.domain.ports.AccountPort;
import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.HoldingSnapshotRepository;
import com.investory.ledger.domain.services.dto.query.GetHoldingsQuery;
import com.investory.ledger.domain.services.dto.result.HoldingListResult;
import com.investory.ledger.domain.services.dto.result.HoldingResult;
import com.investory.ledger.domain.services.dto.result.HoldingSummaryResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HoldingQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final HoldingSnapshotRepository holdingSnapshotRepository;
    private final AccountPort accountPort;
    private final MarketDataPort marketDataPort;

    public HoldingQueryService(HoldingSnapshotRepository holdingSnapshotRepository, AccountPort accountPort, MarketDataPort marketDataPort) {
        this.holdingSnapshotRepository = holdingSnapshotRepository;
        this.accountPort = accountPort;
        this.marketDataPort = marketDataPort;
    }

    public HoldingListResult getHoldings(GetHoldingsQuery query) {
        List<Long> accountIds = resolveAccountIds(query.userId(), query.accountId());
        if (accountIds.isEmpty()) {
            return emptyResult();
        }

        List<Holding> holdings = holdingSnapshotRepository.findLatestByAccountIds(accountIds, query.securityId()).stream()
                .filter(holding -> holding.getQuantity().signum() > 0)
                .collect(Collectors.toList());

        if (holdings.isEmpty()) {
            return emptyResult();
        }

        LocalDate snapshotDate = holdings.stream()
                .map(Holding::getSnapshotDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        Map<Long, List<Holding>> bySecurity = holdings.stream()
                .collect(Collectors.groupingBy(Holding::getSecurityId));
        Map<Long, SecurityInfo> securitiesById = marketDataPort.findSecurities(new ArrayList<>(bySecurity.keySet())).stream()
                .collect(Collectors.toMap(SecurityInfo::securityId, Function.identity()));

        List<AggregatedHolding> aggregated = bySecurity.entrySet().stream()
                .map(entry -> aggregate(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        BigDecimal totalPurchaseAmount = sum(aggregated, AggregatedHolding::purchaseAmount);
        BigDecimal totalMarketValue = sum(aggregated, AggregatedHolding::marketValue);
        BigDecimal totalProfitLossAmount = totalMarketValue.subtract(totalPurchaseAmount);
        BigDecimal totalReturnRate = percentage(totalProfitLossAmount, totalPurchaseAmount);

        List<HoldingResult> holdingResults = aggregated.stream()
                .sorted(Comparator.comparing(AggregatedHolding::marketValue).reversed())
                .map(a -> toHoldingResult(a, securitiesById.get(a.securityId()), totalMarketValue))
                .collect(Collectors.toList());

        HoldingSummaryResult summary = new HoldingSummaryResult(
                holdingResults.size(), totalPurchaseAmount, totalMarketValue, totalProfitLossAmount, totalReturnRate);

        return new HoldingListResult(snapshotDate, summary, holdingResults);
    }

    private List<Long> resolveAccountIds(Long userId, Long accountId) {
        if (accountId != null) {
            AccountInfo account = accountPort.findAccount(accountId, userId)
                    .orElseThrow(() -> new LedgerException(LedgerErrorCode.ACCOUNT_NOT_FOUND));
            return List.of(account.accountId());
        }
        return accountPort.findAccountsByUserId(userId).stream()
                .map(AccountInfo::accountId)
                .collect(Collectors.toList());
    }

    // 통합 평균매입가 = 계좌별 매입금액 합 / 계좌별 수량 합
    private AggregatedHolding aggregate(Long securityId, List<Holding> accountHoldings) {
        BigDecimal quantity = sumHoldings(accountHoldings, Holding::getQuantity);
        BigDecimal purchaseAmount = sumHoldings(accountHoldings, Holding::getPurchaseAmount);
        BigDecimal marketValue = sumHoldings(accountHoldings, Holding::getMarketValue);
        BigDecimal currentPrice = accountHoldings.get(0).getCurrentPrice();
        return new AggregatedHolding(securityId, quantity, purchaseAmount, marketValue, currentPrice);
    }

    private HoldingResult toHoldingResult(AggregatedHolding a, SecurityInfo security, BigDecimal totalMarketValue) {
        BigDecimal averagePurchasePrice = a.quantity().signum() == 0
                ? BigDecimal.ZERO
                : a.purchaseAmount().divide(a.quantity(), MathContext.DECIMAL64);
        BigDecimal profitLossAmount = a.marketValue().subtract(a.purchaseAmount());
        BigDecimal returnRate = percentage(profitLossAmount, a.purchaseAmount());
        BigDecimal portfolioWeight = percentage(a.marketValue(), totalMarketValue);

        return new HoldingResult(
                a.securityId(),
                security != null ? security.securityCode() : null,
                security != null ? security.securityName() : null,
                security != null ? security.marketType() : null,
                security != null ? security.sectorName() : null,
                a.quantity(),
                averagePurchasePrice,
                a.currentPrice(),
                a.purchaseAmount(),
                a.marketValue(),
                profitLossAmount,
                returnRate,
                portfolioWeight
        );
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MathContext.DECIMAL64).multiply(HUNDRED);
    }

    private BigDecimal sum(List<AggregatedHolding> values, Function<AggregatedHolding, BigDecimal> extractor) {
        return values.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumHoldings(List<Holding> values, Function<Holding, BigDecimal> extractor) {
        return values.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private HoldingListResult emptyResult() {
        return new HoldingListResult(null,
                new HoldingSummaryResult(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                List.of());
    }

    private record AggregatedHolding(Long securityId, BigDecimal quantity, BigDecimal purchaseAmount,
                                      BigDecimal marketValue, BigDecimal currentPrice) {
    }
}
