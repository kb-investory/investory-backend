package com.investory.tendency.infra.entities;

import com.investory.tendency.domain.model.AnalysisResultDetail;
import lombok.Data;
import lombok.NoArgsConstructor;

// analysis_results + analysis_dimensions + analysis_types 조인 결과 전용. 저장은 하지 않는 읽기 전용 row.
@Data
@NoArgsConstructor
public class AnalysisResultDetailRow {
    private Long analysisResultId;
    private String dimensionCode;
    private String dimensionName;
    private String typeCode;
    private String typeName;
    private String typeDescription;
    private String evidenceJson;

    public AnalysisResultDetail toDomain() {
        return new AnalysisResultDetail(analysisResultId, dimensionCode, dimensionName, typeCode, typeName, typeDescription, evidenceJson);
    }
}
