package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.Holding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeHoldingSnapshotRepository implements HoldingSnapshotRepository {

    private final List<Holding> holdings = new ArrayList<>();

    public void add(Holding... holdings) {
        this.holdings.addAll(List.of(holdings));
    }

    @Override
    public List<Holding> findLatestByAccountIds(List<Long> accountIds, Long securityId) {
        return holdings.stream()
                .filter(holding -> accountIds.contains(holding.getAccountId()))
                .filter(holding -> securityId == null || securityId.equals(holding.getSecurityId()))
                .collect(Collectors.toList());
    }

    @Override
    public void upsert(Holding holding) {
        holdings.removeIf(existing -> existing.getAccountId().equals(holding.getAccountId())
                && existing.getSecurityId().equals(holding.getSecurityId())
                && existing.getSnapshotDate().equals(holding.getSnapshotDate()));
        holdings.add(holding);
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        holdings.removeIf(holding -> holding.getAccountId().equals(accountId));
    }
}
