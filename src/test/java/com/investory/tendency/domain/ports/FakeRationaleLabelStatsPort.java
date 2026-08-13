package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.constant.RationaleLabelType;

import java.time.LocalDate;
import java.util.Map;

public class FakeRationaleLabelStatsPort implements RationaleLabelStatsPort {

    private Map<RationaleLabelType, Long> counts = Map.of();

    public void setCounts(Map<RationaleLabelType, Long> counts) {
        this.counts = counts;
    }

    @Override
    public Map<RationaleLabelType, Long> countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return counts;
    }
}
