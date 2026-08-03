package com.investory.broker.domain.model;

import com.investory.broker.domain.constant.AccountType;
import lombok.Getter;

@Getter
public class InvestmentAccount {

    private final Long accountId;
    private final Long connectionId;
    private final String externalAccountId;
    private final String accountNoMasked;
    private final String accountName;
    private final AccountType accountType;
    private final String currencyCode;

    private InvestmentAccount(
            Long accountId,
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode) {
        this.accountId = accountId;
        this.connectionId = connectionId;
        this.externalAccountId = externalAccountId;
        this.accountNoMasked = accountNoMasked;
        this.accountName = accountName;
        this.accountType = accountType;
        this.currencyCode = currencyCode;
    }

    public static InvestmentAccount of(
            Long accountId,
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode) {
        return new InvestmentAccount(
                accountId, connectionId, externalAccountId,
                accountNoMasked, accountName, accountType, currencyCode
        );
    }
}
