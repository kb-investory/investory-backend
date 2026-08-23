package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.AnalysisRunStatus;
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
    AnalysisRunStatus runStatus,
    Instant createdAt
) {
    public static AnalysisRunSummaryResponse from(AnalysisRunSummaryResult result) {
        return new AnalysisRunSummaryResponse(result.analysisRunId(), result.periodStart(), result.periodEnd(),
                result.tradeCount(), result.journalCount(), result.analysisVersion(), result.runStatus(), result.createdAt());
    }
}
