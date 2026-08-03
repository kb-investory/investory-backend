package com.investory.ledger.domain.ports;

import com.investory.ledger.domain.ports.dto.AccountInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeAccountPort implements AccountPort {

    private final Map<Long, List<AccountInfo>> accountsByUserId = new HashMap<>();
    private final List<AccountInfo> allAccounts = new ArrayList<>();

    public void add(Long userId, AccountInfo... accounts) {
        accountsByUserId.computeIfAbsent(userId, key -> new ArrayList<>()).addAll(List.of(accounts));
        allAccounts.addAll(List.of(accounts));
    }

    @Override
    public List<AccountInfo> findAccountsByUserId(Long userId) {
        return accountsByUserId.getOrDefault(userId, List.of());
    }

    @Override
    public List<AccountInfo> findAccounts(List<Long> accountIds) {
        return allAccounts.stream()
                .filter(account -> accountIds.contains(account.accountId()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AccountInfo> findAccount(Long accountId, Long userId) {
        return findAccountsByUserId(userId).stream()
                .filter(account -> account.accountId().equals(accountId))
                .findFirst();
    }
}
