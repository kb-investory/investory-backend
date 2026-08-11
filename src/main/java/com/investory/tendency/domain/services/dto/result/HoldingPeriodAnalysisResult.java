package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.HoldingPeriodType;

public record HoldingPeriodAnalysisResult(
        HoldingPeriodType type,
        double threshold,
        int totalCount,
        HoldingPeriodBucketResult shortTerm,
        HoldingPeriodBucketResult mediumTerm,
        HoldingPeriodBucketResult longTerm
) {
}
