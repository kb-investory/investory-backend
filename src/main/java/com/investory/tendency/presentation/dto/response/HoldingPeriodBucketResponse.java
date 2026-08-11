package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.services.dto.result.HoldingPeriodBucketResult;

public record HoldingPeriodBucketResponse(int count, double ratio) {
    public static HoldingPeriodBucketResponse from(HoldingPeriodBucketResult result) {
        return new HoldingPeriodBucketResponse(result.count(), result.ratio());
    }
}
