package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountDetailResult(
    Long accountId,
    Long connectionId,
    Long brokerId,
    String brokerName,
    String accountNoMasked,
    String accountName,
    AccountType accountType,
    Instant lastSyncedAt,
    Summary summary,
    List<HoldingDetail> holdings
) {
    public record Summary(
        int holdingCount,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnl
    ) {
    }

    public record HoldingDetail(
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
    }
}
