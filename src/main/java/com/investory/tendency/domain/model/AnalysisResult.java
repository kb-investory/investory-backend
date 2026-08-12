package com.investory.tendency.domain.model;

import lombok.Getter;

import java.time.Instant;

// 실행(analysis_run) 하나 안의 항목(dimension) 하나에 대한 판정 결과.
// evidenceJson은 서비스 계층이 이미 문자열(JSON)로 직렬화해서 넘긴 값을 그대로 보관한다 —
// 이 모델은 그 구조를 해석하지 않는다.
@Getter
public class AnalysisResult {

    private final Long analysisResultId;
    private final Long analysisRunId;
    private final String analysisDimensionCode;
    private final String primaryAnalysisTypeCode;
    private final String evidenceJson;
    private final Instant createdAt;

    private AnalysisResult(Long analysisResultId, Long analysisRunId, String analysisDimensionCode,
                            String primaryAnalysisTypeCode, String evidenceJson, Instant createdAt) {
        this.analysisResultId = analysisResultId;
        this.analysisRunId = analysisRunId;
        this.analysisDimensionCode = analysisDimensionCode;
        this.primaryAnalysisTypeCode = primaryAnalysisTypeCode;
        this.evidenceJson = evidenceJson;
        this.createdAt = createdAt;
    }

    public static AnalysisResult create(Long analysisRunId, String analysisDimensionCode,
                                         String primaryAnalysisTypeCode, String evidenceJson) {
        return new AnalysisResult(null, analysisRunId, analysisDimensionCode, primaryAnalysisTypeCode, evidenceJson, null);
    }

    public static AnalysisResult of(Long analysisResultId, Long analysisRunId, String analysisDimensionCode,
                                     String primaryAnalysisTypeCode, String evidenceJson, Instant createdAt) {
        return new AnalysisResult(analysisResultId, analysisRunId, analysisDimensionCode, primaryAnalysisTypeCode, evidenceJson, createdAt);
    }
}
