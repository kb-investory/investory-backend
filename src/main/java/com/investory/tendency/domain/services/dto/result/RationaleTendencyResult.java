package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.RationaleTendencyResultType;

public record RationaleTendencyResult(
        double fundamental,
        double priceTrend,
        double event,
        double intuition,
        double unclassified,
        double threshold,
        RationaleTendencyResultType result
) {
}
