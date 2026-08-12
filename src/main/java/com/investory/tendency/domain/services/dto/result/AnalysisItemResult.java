package com.investory.tendency.domain.services.dto.result;

public record AnalysisItemResult(
    String dimensionCode,
    String dimensionName,
    String typeCode,
    String typeName,
    String typeDescription,
    String evidenceJson
) {
}
