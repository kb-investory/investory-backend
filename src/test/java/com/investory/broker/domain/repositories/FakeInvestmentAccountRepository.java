package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.AccountType;
import com.investory.broker.domain.model.InvestmentAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeInvestmentAccountRepository implements InvestmentAccountRepository {

    private record Owned(Long userId, InvestmentAccount account) {
    }

    private final List<Owned> accounts = new ArrayList<>();
    private long nextAccountId = 1000L;

    public void add(Long userId, InvestmentAccount account) {
        accounts.add(new Owned(userId, account));
    }

    @Override
    public Long insert(
            Long connectionId,
            String externalAccountId,
            String accountNoMasked,
            String accountName,
            AccountType accountType,
            String currencyCode) {
        Long accountId = nextAccountId++;
        InvestmentAccount account = InvestmentAccount.of(
                accountId, connectionId, externalAccountId, accountNoMasked, accountName, accountType, currencyCode);
        accounts.add(new Owned(null, account));
        return accountId;
    }

    @Override
    public List<InvestmentAccount> findByConnectionId(Long connectionId) {
        return accounts.stream()
                .map(Owned::account)
                .filter(account -> account.getConnectionId().equals(connectionId))
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestmentAccount> findByUserId(Long userId) {
        return accounts.stream()
                .filter(owned -> userId.equals(owned.userId()))
                .map(Owned::account)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestmentAccount> findByIds(List<Long> accountIds) {
        return accounts.stream()
                .map(Owned::account)
                .filter(account -> accountIds.contains(account.getAccountId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InvestmentAccount> findByIdAndUserId(Long accountId, Long userId) {
        return accounts.stream()
                .filter(owned -> userId.equals(owned.userId()))
                .map(Owned::account)
                .filter(account -> account.getAccountId().equals(accountId))
                .findFirst();
    }
}
