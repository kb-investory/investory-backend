package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.AccountType;

import java.util.ArrayList;
import java.util.List;

public class FakeInvestmentAccountRepository implements InvestmentAccountRepository {

    public record Inserted(
        Long connectionId,
        String externalAccountId,
        String accountNoMasked,
        String accountName,
        AccountType accountType,
        String currencyCode
    ) {
    }

    public final List<Inserted> inserted = new ArrayList<>();
    private long nextAccountId = 1000L;

    @Override
    public Long insert(
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode) {
        inserted.add(new Inserted(connectionId, externalAccountId, accountNoMasked, accountName, accountType, currencyCode));
        return nextAccountId++;
    }
}
