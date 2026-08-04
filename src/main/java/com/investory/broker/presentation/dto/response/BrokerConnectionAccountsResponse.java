package com.investory.broker.presentation.dto.response;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.services.dto.result.ConnectionAccountsResult;

import java.util.List;
import java.util.stream.Collectors;

public record BrokerConnectionAccountsResponse(
    Long connectionId,
    Long brokerId,
    String brokerName,
    List<AccountResponse> accounts
) {
    public static BrokerConnectionAccountsResponse from(ConnectionAccountsResult result) {
        List<AccountResponse> accounts = result.accounts().stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
        return new BrokerConnectionAccountsResponse(result.connectionId(), result.brokerId(), result.brokerName(), accounts);
    }

    public record AccountResponse(
        Long accountId,
        String accountNoMasked,
        String accountName,
        AccountType accountType,
        int holdingCount,
        long totalMarketValue,
        long totalUnrealizedPnl
    ) {
        public static AccountResponse from(ConnectionAccountsResult.AccountSummary summary) {
            return new AccountResponse(
                    summary.accountId(),
                    summary.accountNoMasked(),
                    summary.accountName(),
                    summary.accountType(),
                    summary.holdingCount(),
                    summary.totalMarketValue().longValue(),
                    summary.totalUnrealizedPnl().longValue()
            );
        }
    }
}
