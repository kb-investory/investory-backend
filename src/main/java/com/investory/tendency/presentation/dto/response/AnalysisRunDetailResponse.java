package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;

import java.util.List;
import java.util.stream.Collectors;

public record AnalysisRunDetailResponse(
    AnalysisRunSummaryResponse run,
    List<AnalysisItemResponse> items,
    String errorMessage
) {
    public static AnalysisRunDetailResponse from(AnalysisRunDetailResult result) {
        List<AnalysisItemResponse> items = result.items().stream()
                .map(AnalysisItemResponse::from)
                .collect(Collectors.toList());
        return new AnalysisRunDetailResponse(AnalysisRunSummaryResponse.from(result.run()), items, result.errorMessage());
    }
}
