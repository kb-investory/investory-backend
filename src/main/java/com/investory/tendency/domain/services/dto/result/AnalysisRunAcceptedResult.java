package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.AnalysisRunStatus;

// POST /tendency/analyses의 202 응답용 — 실제 분석 결과는 담지 않는다(백그라운드에서 처리되므로).
// GET /tendency/analyses/{analysisRunId}로 폴링해 진행 상태·결과를 조회한다.
public record AnalysisRunAcceptedResult(
    Long analysisRunId,
    AnalysisRunStatus runStatus
) {
}
