package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.Trade;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeTradeRepository implements TradeRepository {

    // 실제 TradeRepositoryImpl과 동일하게 from/to 날짜 경계를 KST 기준으로 해석한다.
    private static final ZoneId JOURNAL_ZONE = ZoneId.of("Asia/Seoul");

    private final List<Trade> trades = new ArrayList<>();
    private long nextId = 1L;

    public void add(Trade... trades) {
        for (Trade trade : trades) {
            this.trades.add(withId(trade));
        }
    }

    @Override
    public List<Trade> search(TradeSearchCriteria criteria) {
        List<Trade> filtered = filter(criteria);
        filtered.sort(Comparator.comparing(Trade::getTradedAt).reversed());
        int fromIndex = Math.min(criteria.page() * criteria.size(), filtered.size());
        int toIndex = Math.min(fromIndex + criteria.size(), filtered.size());
        return filtered.subList(fromIndex, toIndex);
    }

    @Override
    public long count(TradeSearchCriteria criteria) {
        return filter(criteria).size();
    }

    private List<Trade> filter(TradeSearchCriteria criteria) {
        return trades.stream()
                .filter(trade -> criteria.accountIds().contains(trade.getAccountId()))
                .filter(trade -> criteria.securityId() == null || criteria.securityId().equals(trade.getSecurityId()))
                .filter(trade -> criteria.tradeSide() == null || criteria.tradeSide() == trade.getTradeSide())
                .filter(trade -> criteria.from() == null || !toDate(trade).isBefore(criteria.from()))
                .filter(trade -> criteria.to() == null || !toDate(trade).isAfter(criteria.to()))
                .collect(Collectors.toList());
    }

    private LocalDate toDate(Trade trade) {
        return trade.getTradedAt().atZone(JOURNAL_ZONE).toLocalDate();
    }

    @Override
    public Optional<Trade> findById(Long tradeId) {
        return trades.stream().filter(trade -> trade.getTradeId().equals(tradeId)).findFirst();
    }

    @Override
    public Optional<Trade> findByAccountIdAndExternalTradeId(Long accountId, String externalTradeId) {
        return trades.stream()
                .filter(trade -> trade.getAccountId().equals(accountId) && trade.getExternalTradeId().equals(externalTradeId))
                .findFirst();
    }

    @Override
    public List<Trade> findAllByAccountIdAndSecurityId(Long accountId, Long securityId) {
        return trades.stream()
                .filter(trade -> trade.getAccountId().equals(accountId) && trade.getSecurityId().equals(securityId))
                .sorted(Comparator.comparing(Trade::getTradedAt))
                .collect(Collectors.toList());
    }

    @Override
    public Trade save(Trade trade) {
        Trade saved = withId(trade);
        trades.add(saved);
        return saved;
    }

    @Override
    public List<Long> findTradeIdsByAccountId(Long accountId) {
        return trades.stream()
                .filter(trade -> trade.getAccountId().equals(accountId))
                .map(Trade::getTradeId)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        trades.removeIf(trade -> trade.getAccountId().equals(accountId));
    }

    private Trade withId(Trade trade) {
        return Trade.of(nextId++, trade.getAccountId(), trade.getSecurityId(), trade.getTradeSide(),
                trade.getQuantity(), trade.getUnitPrice(), trade.getTransactionCostAmount(),
                trade.getExternalTradeId(), trade.getTradedAt());
    }
}
