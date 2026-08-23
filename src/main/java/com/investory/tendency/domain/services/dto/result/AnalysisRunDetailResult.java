package com.investory.tendency.domain.services.dto.result;

import java.util.List;

public record AnalysisRunDetailResult(
    AnalysisRunSummaryResult run,
    List<AnalysisItemResult> items,
    String errorMessage
) {
}
