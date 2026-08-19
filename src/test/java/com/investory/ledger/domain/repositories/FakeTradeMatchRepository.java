package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.TradeMatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FakeTradeMatchRepository implements TradeMatchRepository {

    private final List<TradeMatch> matches = new ArrayList<>();
    private final List<Long> deletedByAccountIdCalls = new ArrayList<>();
    private int deleteCallCount = 0;

    @Override
    public void deleteByAccountIdAndSecurityId(Long accountId, Long securityId) {
        deleteCallCount++;
        matches.removeIf(match -> match.getSecurityId().equals(securityId));
    }

    // TradeMatch 도메인 모델에 accountId가 없어(§ledger DB에서만 buyTradeId로 trades를 조인해 판단) 이
    // 페이크는 정밀하게 계좌별로 거를 수 없다 — 계좌 하나만 다루는 테스트 시나리오를 가정하고 전부 지운다.
    @Override
    public void deleteByAccountId(Long accountId) {
        deletedByAccountIdCalls.add(accountId);
        matches.clear();
    }

    public List<Long> deletedByAccountIdCalls() {
        return deletedByAccountIdCalls;
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
