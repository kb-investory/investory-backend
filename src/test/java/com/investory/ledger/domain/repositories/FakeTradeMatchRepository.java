package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.TradeMatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FakeTradeMatchRepository implements TradeMatchRepository {

    private final List<TradeMatch> matches = new ArrayList<>();
    private int deleteCallCount = 0;

    @Override
    public void deleteByAccountIdAndSecurityId(Long accountId, Long securityId) {
        deleteCallCount++;
        matches.removeIf(match -> match.getSecurityId().equals(securityId));
    }

    @Override
    public void saveAll(List<TradeMatch> newMatches) {
        matches.addAll(newMatches);
    }

    @Override
    public List<Integer> findHoldingDaysByAccountIdsSince(List<Long> accountIds, Instant since) {
        return List.of();
    }

    public List<TradeMatch> all() {
        return matches;
    }

    public int deleteCallCount() {
        return deleteCallCount;
    }
}
