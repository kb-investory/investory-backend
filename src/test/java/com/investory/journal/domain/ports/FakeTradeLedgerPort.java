package com.investory.journal.domain.ports;

import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeTradeLedgerPort implements TradeLedgerPort {

    private final List<TradeCountInfo> tradeCounts = new ArrayList<>();
    private final List<TradeInfo> trades = new ArrayList<>();

    public void add(TradeCountInfo... tradeCounts) {
        this.tradeCounts.addAll(List.of(tradeCounts));
    }

    public void add(TradeInfo... trades) {
        this.trades.addAll(List.of(trades));
    }

    @Override
    public List<TradeCountInfo> countTradesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return tradeCounts.stream()
                .filter(info -> !info.tradeDate().isBefore(startDate) && !info.tradeDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeInfo> findTradesOn(Long userId, LocalDate date) {
        return trades.stream()
                .filter(trade -> LocalDate.ofInstant(trade.tradedAt(), ZoneOffset.UTC).isEqual(date))
                .collect(Collectors.toList());
    }
}
