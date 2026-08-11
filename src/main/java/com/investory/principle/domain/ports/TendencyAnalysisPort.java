package com.investory.principle.domain.ports;

import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;

import java.util.List;
import java.util.Optional;

// principle -> TendencyAnalysisPort -> tendency ("추천 기준이 되는 분석 결과").
// tendency의 analysis_runs/analysis_results 영속성이 아직 없어, 현재 구현체(infra/port_impls)는
// 고정값을 반환하는 임시 어댑터다. tendency가 실제 분석 결과를 영속화하면 그 구현체만 교체하면 된다.
public interface TendencyAnalysisPort {

    // 실행(run) 1건은 항목(analysis_dimension_code)별로 결과가 여러 개(최대 6개) 나온다 — 그래서 목록으로 반환한다.
    // GET /api/principle/recommendations에서 사용자의 최신 실행에 속한 항목별 결과를 모두 조회할 때 쓴다.
    List<TendencyAnalysisInfo> findLatestCompletedAnalysisResults(Long userId);

    // GET /api/principle 에서 과거에 채택된 추천 원칙의 origin.analysisTypeName을 복원할 때 사용한다.
    Optional<TendencyAnalysisInfo> findAnalysisType(Long analysisResultId);
}
