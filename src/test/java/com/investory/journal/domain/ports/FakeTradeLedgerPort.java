package com.investory.journal.domain.ports;

import com.investory.journal.domain.ports.dto.TradeCountInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeTradeLedgerPort implements TradeLedgerPort {

    private final List<TradeCountInfo> tradeCounts = new ArrayList<>();

    public void add(TradeCountInfo... tradeCounts) {
        this.tradeCounts.addAll(List.of(tradeCounts));
    }

    @Override
    public List<TradeCountInfo> countTradesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return tradeCounts.stream()
                .filter(info -> !info.tradeDate().isBefore(startDate) && !info.tradeDate().isAfter(endDate))
                .collect(Collectors.toList());
    }
}
