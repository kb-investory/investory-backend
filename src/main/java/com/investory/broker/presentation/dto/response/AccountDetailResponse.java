package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.services.dto.result.AccountDetailResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record AccountDetailResponse(
    Long accountId,
    Long connectionId,
    Long brokerId,
    String brokerName,
    String accountNoMasked,
    String accountName,
    AccountType accountType,
    Instant lastSyncedAt,
    Summary summary,
    List<HoldingResponse> holdings
) {
    public static AccountDetailResponse from(AccountDetailResult result) {
        List<HoldingResponse> holdings = result.holdings().stream()
                .map(HoldingResponse::from)
                .collect(Collectors.toList());
        return new AccountDetailResponse(
                result.accountId(),
                result.connectionId(),
                result.brokerId(),
                result.brokerName(),
                result.accountNoMasked(),
                result.accountName(),
                result.accountType(),
                result.lastSyncedAt(),
                Summary.from(result.summary()),
                holdings
        );
    }

    public record Summary(
        int holdingCount,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnl
    ) {
        public static Summary from(AccountDetailResult.Summary summary) {
            return new Summary(summary.holdingCount(), summary.totalMarketValue(), summary.totalUnrealizedPnl());
        }
    }

    public record HoldingResponse(
        Long securityId,
        String securityCode,
        String securityName,
        String marketType,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal portfolioWeight,
        LocalDate snapshotDate
    ) {
        public static HoldingResponse from(AccountDetailResult.HoldingDetail holding) {
            return new HoldingResponse(
                    holding.securityId(),
                    holding.securityCode(),
                    holding.securityName(),
                    holding.marketType(),
                    holding.quantity(),
                    holding.averageCost(),
                    holding.marketValue(),
                    holding.unrealizedPnl(),
                    holding.portfolioWeight(),
                    holding.snapshotDate()
            );
        }
    }
}
