package com.investory.principle.domain.ports.dto;

public record TendencyAnalysisInfo(
        Long analysisResultId,
        Long analysisRunId,
        String analysisTypeCode,
        String analysisTypeName
) {
}
