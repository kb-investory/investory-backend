package com.investory.tendency.domain.repositories;

import com.investory.tendency.domain.model.AnalysisRun;

import java.util.List;
import java.util.Optional;

public interface AnalysisRunRepository {

    // analysisRunId가 채워진 상태로 반환 (insert 후 생성된 PK 포함). REQUESTED 상태로 저장된다.
    AnalysisRun save(AnalysisRun analysisRun);

    // 유저의 실행 이력, 최신순
    List<AnalysisRun> findByUserId(Long userId);

    // 상세 조회용 — 소유자 검증까지 쿼리에 포함 (남의 것이면 empty)
    Optional<AnalysisRun> findByIdAndUserId(Long analysisRunId, Long userId);

    // 계정 탈퇴 시 — 호출 전에 이 사용자의 analysis_results가 이미 지워졌다고 가정한다.
    void deleteByUserId(Long userId);

    // 백그라운드 워커가 실제로 처리를 시작했을 때
    void markRunning(Long analysisRunId);

    // 백그라운드 워커가 정상 완료했을 때 — 이 시점에야 실제 tradeCount/journalCount를 채운다.
    void markSuccess(Long analysisRunId, int tradeCount, int journalCount);

    // 백그라운드 워커가 실패했을 때(작업 제출 자체가 거부된 경우 포함)
    void markFailed(Long analysisRunId, String errorMessage);

    // 새 분석 요청을 받기 전 중복 실행 방지용 — REQUESTED/RUNNING 상태의 실행이 있는지
    boolean existsInProgressByUserId(Long userId);
}
