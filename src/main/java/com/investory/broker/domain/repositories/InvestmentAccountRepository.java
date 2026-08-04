package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.InvestmentAccount;

import java.util.List;
import java.util.Optional;

public interface InvestmentAccountRepository {
    Long insert(
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode
    );

    List<InvestmentAccount> findByConnectionId(Long connectionId);

    List<InvestmentAccount> findByUserId(Long userId);

    List<InvestmentAccount> findByIds(List<Long> accountIds);

    Optional<InvestmentAccount> findByIdAndUserId(Long accountId, Long userId);
}
