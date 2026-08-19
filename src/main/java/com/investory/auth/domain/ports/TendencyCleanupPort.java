package com.investory.auth.domain.ports;

// tendency.domain.services.AnalysisRunService.deleteAllAnalyses(Long)로 위임 예정.
// 계정 탈퇴 시 사용자의 성향분석 기록(analysis_runs/analysis_results)을 전부 지운다.
public interface TendencyCleanupPort {
    void deleteAllAnalyses(Long userId);
}
