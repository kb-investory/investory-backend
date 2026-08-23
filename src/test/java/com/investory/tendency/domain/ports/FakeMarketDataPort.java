package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.DailyPriceInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FakeMarketDataPort implements MarketDataPort {

    private final Map<Long, List<DailyPriceInfo>> pricesBySecurity = new HashMap<>();

    public void addPrice(Long securityId, LocalDate date, double closePrice) {
        pricesBySecurity.computeIfAbsent(securityId, k -> new ArrayList<>())
                .add(new DailyPriceInfo(date, java.math.BigDecimal.valueOf(closePrice), null));
    }

    @Override
    public List<DailyPriceInfo> findDailyPrices(Long securityId, LocalDate from, LocalDate to) {
        return pricesBySecurity.getOrDefault(securityId, List.of()).stream()
                .filter(p -> !p.priceDate().isBefore(from) && !p.priceDate().isAfter(to))
                .toList();
    }

    @Override
    public Map<Long, List<DailyPriceInfo>> findDailyPrices(List<Long> securityIds, LocalDate from, LocalDate to) {
        Map<Long, List<DailyPriceInfo>> result = new HashMap<>();
        for (Long securityId : securityIds) {
            List<DailyPriceInfo> prices = findDailyPrices(securityId, from, to);
            if (!prices.isEmpty()) {
                result.put(securityId, prices);
            }
        }
        return result;
    }
}
