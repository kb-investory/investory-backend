package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.AnalysisRunStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AnalysisRunSummaryResult(
    Long analysisRunId,
    LocalDate periodStart,
    LocalDate periodEnd,
    int tradeCount,
    int journalCount,
    String analysisVersion,
    AnalysisRunStatus runStatus,
    Instant createdAt
) {
}
