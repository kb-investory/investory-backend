package com.investory.tendency.domain.ports;

import java.util.List;

// principle.domain.services.PrincipleService.deleteRecommendationsForAnalysisResults(List<Long>)로
// 위임 예정. 계정 탈퇴로 analysis_results가 삭제될 때, 그 결과를 참조하는 principle_recommendations를
// 먼저 지운다 — 안 그러면 조회 경로가 사라진 고아 데이터로 남는다.
public interface PrincipleRecommendationCleanupPort {
    void deleteRecommendationsForAnalysisResults(List<Long> analysisResultIds);
}
