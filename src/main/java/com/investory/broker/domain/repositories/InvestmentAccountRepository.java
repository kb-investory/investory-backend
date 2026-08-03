package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.AccountType;

public interface InvestmentAccountRepository {
    Long insert(
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode
    );
}
