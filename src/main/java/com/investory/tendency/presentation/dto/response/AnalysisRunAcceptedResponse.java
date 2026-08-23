package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.AnalysisRunStatus;
import com.investory.tendency.domain.services.dto.result.AnalysisRunAcceptedResult;

public record AnalysisRunAcceptedResponse(
    Long analysisRunId,
    AnalysisRunStatus runStatus
) {
    public static AnalysisRunAcceptedResponse from(AnalysisRunAcceptedResult result) {
        return new AnalysisRunAcceptedResponse(result.analysisRunId(), result.runStatus());
    }
}
