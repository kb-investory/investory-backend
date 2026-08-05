package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.services.dto.result.AccountListResult;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record AccountListResponse(
    Summary summary,
    List<AccountResponse> accounts
) {
    public static AccountListResponse from(AccountListResult result) {
        List<AccountResponse> accounts = result.accounts().stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
        return new AccountListResponse(Summary.from(result.summary()), accounts);
    }

    public record Summary(
        int accountCount,
        long totalMarketValue,
        long totalUnrealizedPnl
    ) {
        public static Summary from(AccountListResult.AccountsSummary summary) {
            return new Summary(
                    summary.accountCount(),
                    summary.totalMarketValue().longValue(),
                    summary.totalUnrealizedPnl().longValue()
            );
        }
    }

    public record AccountResponse(
        Long accountId,
        Long connectionId,
        Long brokerId,
        String brokerName,
        String accountNoMasked,
        String accountName,
        AccountType accountType,
        int holdingCount,
        long totalMarketValue,
        long totalUnrealizedPnl,
        Instant lastSyncedAt
    ) {
        public static AccountResponse from(AccountListResult.AccountResult account) {
            return new AccountResponse(
                    account.accountId(),
                    account.connectionId(),
                    account.brokerId(),
                    account.brokerName(),
                    account.accountNoMasked(),
                    account.accountName(),
                    account.accountType(),
                    account.holdingCount(),
                    account.totalMarketValue().longValue(),
                    account.totalUnrealizedPnl().longValue(),
                    account.lastSyncedAt()
            );
        }
    }
}
