package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.services.dto.result.AnalysisRunSummaryResult;

import java.util.List;
import java.util.stream.Collectors;

public record AnalysisRunListResponse(
    List<AnalysisRunSummaryResponse> runs
) {
    public static AnalysisRunListResponse from(List<AnalysisRunSummaryResult> results) {
        return new AnalysisRunListResponse(results.stream()
                .map(AnalysisRunSummaryResponse::from)
                .collect(Collectors.toList()));
    }
}
