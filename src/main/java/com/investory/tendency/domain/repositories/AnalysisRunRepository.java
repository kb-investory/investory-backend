package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisRun;

import java.util.List;
import java.util.Optional;

public interface AnalysisRunRepository {

    // analysisRunId가 채워진 상태로 반환 (insert 후 생성된 PK 포함)
    AnalysisRun save(AnalysisRun analysisRun);

    // 유저의 실행 이력, 최신순
    List<AnalysisRun> findByUserId(Long userId);

    // 상세 조회용 — 소유자 검증까지 쿼리에 포함 (남의 것이면 empty)
    Optional<AnalysisRun> findByIdAndUserId(Long analysisRunId, Long userId);

    // 계정 탈퇴 시 — 호출 전에 이 사용자의 analysis_results가 이미 지워졌다고 가정한다.
    void deleteByUserId(Long userId);
}
