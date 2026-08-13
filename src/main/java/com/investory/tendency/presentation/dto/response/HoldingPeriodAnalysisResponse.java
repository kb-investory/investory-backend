package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.HoldingPeriodType;
import com.investory.tendency.domain.services.dto.result.HoldingPeriodAnalysisResult;

public record HoldingPeriodAnalysisResponse(
        HoldingPeriodType label,
        String labelName,
        double threshold,
        int totalCount,
        HoldingPeriodBucketResponse shortTerm,
        HoldingPeriodBucketResponse mediumTerm,
        HoldingPeriodBucketResponse longTerm
) {
    public static HoldingPeriodAnalysisResponse from(HoldingPeriodAnalysisResult result) {
        return new HoldingPeriodAnalysisResponse(
                result.type(),
                result.type().getLabelName(),
                result.threshold(),
                result.totalCount(),
                HoldingPeriodBucketResponse.from(result.shortTerm()),
                HoldingPeriodBucketResponse.from(result.mediumTerm()),
                HoldingPeriodBucketResponse.from(result.longTerm())
        );
    }
}
