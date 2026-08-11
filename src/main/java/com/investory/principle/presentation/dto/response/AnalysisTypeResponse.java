package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.AnalysisTypeResult;

public record AnalysisTypeResponse(
        String code,
        String name
) {
    public static AnalysisTypeResponse from(AnalysisTypeResult result) {
        return new AnalysisTypeResponse(result.code(), result.name());
    }
}
