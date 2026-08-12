package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.services.dto.result.AnalysisRunSummaryResult;

import java.time.Instant;
import java.time.LocalDate;

public record AnalysisRunSummaryResponse(
    Long analysisRunId,
    LocalDate periodStart,
    LocalDate periodEnd,
    int tradeCount,
    int journalCount,
    String analysisVersion,
    Instant createdAt
) {
    public static AnalysisRunSummaryResponse from(AnalysisRunSummaryResult result) {
        return new AnalysisRunSummaryResponse(result.analysisRunId(), result.periodStart(), result.periodEnd(),
                result.tradeCount(), result.journalCount(), result.analysisVersion(), result.createdAt());
    }
}
