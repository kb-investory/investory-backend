package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.services.dto.result.AnalysisItemResult;

public record AnalysisItemResponse(
    String dimensionCode,
    String dimensionName,
    String typeCode,
    String typeName,
    String typeDescription,
    String evidenceJson
) {
    public static AnalysisItemResponse from(AnalysisItemResult result) {
        return new AnalysisItemResponse(result.dimensionCode(), result.dimensionName(),
                result.typeCode(), result.typeName(), result.typeDescription(), result.evidenceJson());
    }
}
