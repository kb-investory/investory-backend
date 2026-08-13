package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.TradeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FakeTradeLedgerPort implements TradeLedgerPort {

    private final List<TradeInfo> trades = new ArrayList<>();

    public void add(TradeInfo... trades) {
        this.trades.addAll(List.of(trades));
    }

    @Override
    public List<TradeInfo> findTrades(Long userId, Long securityId) {
        return trades.stream()
                .filter(t -> t.securityId().equals(securityId))
                .sorted(Comparator.comparing(TradeInfo::tradedAt))
                .toList();
    }

    @Override
    public List<TradeInfo> findAllTrades(Long userId) {
        return trades.stream()
                .sorted(Comparator.comparing(TradeInfo::tradedAt))
                .toList();
    }
}
