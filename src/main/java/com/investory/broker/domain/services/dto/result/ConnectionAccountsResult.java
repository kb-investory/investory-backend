package com.investory.broker.domain.services.dto.result;

import com.investory.broker.domain.constant.AccountType;

import java.math.BigDecimal;
import java.util.List;

public record ConnectionAccountsResult(
    Long connectionId,
    Long brokerId,
    String brokerName,
    List<AccountSummary> accounts
) {
    public record AccountSummary(
        Long accountId,
        String accountNoMasked,
        String accountName,
        AccountType accountType,
        int holdingCount,
        BigDecimal totalMarketValue,
        BigDecimal totalUnrealizedPnl
    ) {
    }
}
