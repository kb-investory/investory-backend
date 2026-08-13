package com.investory.journal.domain.ports;

import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;
import com.investory.journal.domain.ports.dto.TradeTimelineInfo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FakeTradeLedgerPort implements TradeLedgerPort {

    // 실제 TradeLedgerPortImpl과 동일하게 날짜 판단을 KST 기준으로 한다.
    private static final ZoneId JOURNAL_ZONE = ZoneId.of("Asia/Seoul");

    private record TimelineEntry(Long securityId, TradeTimelineInfo trade) {
    }

    private final List<TradeCountInfo> tradeCounts = new ArrayList<>();
    private final List<TradeInfo> trades = new ArrayList<>();
    private final List<TimelineEntry> timelineEntries = new ArrayList<>();

    public void add(TradeCountInfo... tradeCounts) {
        this.tradeCounts.addAll(List.of(tradeCounts));
    }

    public void add(TradeInfo... trades) {
        this.trades.addAll(List.of(trades));
    }

    public void add(Long securityId, TradeTimelineInfo... trades) {
        for (TradeTimelineInfo trade : trades) {
            this.timelineEntries.add(new TimelineEntry(securityId, trade));
        }
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
                .filter(trade -> LocalDate.ofInstant(trade.tradedAt(), JOURNAL_ZONE).isEqual(date))
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeTimelineInfo> findTradesBySecurity(Long userId, Long securityId, LocalDate startDate, LocalDate endDate, int page, int size) {
        List<TradeTimelineInfo> filtered = filterTimelineEntries(securityId, startDate, endDate);
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return filtered.subList(fromIndex, toIndex);
    }

    @Override
    public long countTradesBySecurity(Long userId, Long securityId, LocalDate startDate, LocalDate endDate) {
        return filterTimelineEntries(securityId, startDate, endDate).size();
    }

    private List<TradeTimelineInfo> filterTimelineEntries(Long securityId, LocalDate startDate, LocalDate endDate) {
        return timelineEntries.stream()
                .filter(entry -> entry.securityId().equals(securityId))
                .filter(entry -> startDate == null || !LocalDate.ofInstant(entry.trade().tradedAt(), JOURNAL_ZONE).isBefore(startDate))
                .filter(entry -> endDate == null || !LocalDate.ofInstant(entry.trade().tradedAt(), JOURNAL_ZONE).isAfter(endDate))
                .map(TimelineEntry::trade)
                .sorted(Comparator.comparing(TradeTimelineInfo::tradedAt).reversed())
                .collect(Collectors.toList());
    }
}
