package com.investory.tendency.infra.entities;

import com.investory.tendency.domain.model.AnalysisResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class AnalysisResultRow {
    private Long analysisResultId;
    private Long analysisRunId;
    private String analysisDimensionCode;
    private String primaryAnalysisTypeCode;
    private String evidenceJson;
    private Instant createdAt;

    public static AnalysisResultRow from(AnalysisResult result) {
        AnalysisResultRow row = new AnalysisResultRow();
        row.analysisResultId = result.getAnalysisResultId();
        row.analysisRunId = result.getAnalysisRunId();
        row.analysisDimensionCode = result.getAnalysisDimensionCode();
        row.primaryAnalysisTypeCode = result.getPrimaryAnalysisTypeCode();
        row.evidenceJson = result.getEvidenceJson();
        row.createdAt = result.getCreatedAt();
        return row;
    }

    public AnalysisResult toDomain() {
        return AnalysisResult.of(analysisResultId, analysisRunId, analysisDimensionCode, primaryAnalysisTypeCode, evidenceJson, createdAt);
    }
}
