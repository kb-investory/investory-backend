package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AccountListResult(
    AccountsSummary summary,
    List<AccountResult> accounts
) {
    public static AccountListResult empty() {
        return new AccountListResult(new AccountsSummary(0, BigDecimal.ZERO, BigDecimal.ZERO), List.of());
    }

    public record AccountsSummary(
        int accountCount,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnl
    ) {
    }

    public record AccountResult(
        Long accountId,
        Long connectionId,
        Long brokerId,
        String brokerName,
        String accountNoMasked,
        String accountName,
        AccountType accountType,
        int holdingCount,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnl,
        Instant lastSyncedAt
    ) {
    }
}
