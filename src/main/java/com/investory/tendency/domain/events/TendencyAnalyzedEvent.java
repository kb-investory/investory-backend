package com.investory.tendency.domain.events;

import java.util.List;

// tendency가 성향 분석 실행(analysis_run) 1건을 완료했을 때 발행하는 이벤트.
// tendency -> TendencyAnalyzedEvent -> notification. principle도 이 이벤트를 구독해(infra/listeners)
// 추천 후보를 갱신한다.
//
// 실행 1건은 항목(analysis_dimension_code)별로 결과가 여러 개(최대 6개) 나오므로 results가 목록이다 —
// analysis_results가 (analysis_run_id, analysis_dimension_code) 단위로 여러 행 생기는 것과 대응된다.
//
// 아직 tendency는 analysis_runs/analysis_results 영속성이 없어 이 이벤트를 실제로 발행하는 코드는 없다 —
// tendency가 실행 결과를 저장하는 지점에서 ApplicationEventPublisher.publishEvent(new TendencyAnalyzedEvent(...))로
// 발행하면 된다.
public record TendencyAnalyzedEvent(
        Long userId,
        Long analysisRunId,
        List<AnalysisResult> results
) {
    public record AnalysisResult(
            Long analysisResultId,
            String analysisDimensionCode,
            String analysisTypeCode,
            String analysisTypeName
    ) {
    }
}
