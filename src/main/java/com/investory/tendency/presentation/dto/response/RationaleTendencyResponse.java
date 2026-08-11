package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.RationaleTendencyResultType;
import com.investory.tendency.domain.services.dto.result.RationaleTendencyResult;

public record RationaleTendencyResponse(
        double fundamental,
        double priceTrend,
        double event,
        double intuition,
        double unclassified,
        double threshold,
        RationaleTendencyResultType result
) {
    public static RationaleTendencyResponse from(RationaleTendencyResult result) {
        return new RationaleTendencyResponse(
                result.fundamental(),
                result.priceTrend(),
                result.event(),
                result.intuition(),
                result.unclassified(),
                result.threshold(),
                result.result()
        );
    }
}
