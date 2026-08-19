package com.investory.principle.domain.model;

import com.investory.principle.domain.constant.RecommendationGenerationStatus;
import lombok.Getter;

import java.time.Instant;

// analysisRunId 1건당 정확히 1행 — tendency의 분석 완료 이벤트를 받아 principle이 추천을 생성하는
// 작업 자체의 생명주기를 추적한다. account_sync_batches(REQUESTED → SUCCESS|FAILED)와 같은 어휘.
@Getter
public class RecommendationGeneration {

    private final Long analysisRunId;
    private final Long userId;
    private final RecommendationGenerationStatus status;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant completedAt;

    private RecommendationGeneration(Long analysisRunId, Long userId, RecommendationGenerationStatus status,
                                      String errorMessage, Instant createdAt, Instant completedAt) {
        this.analysisRunId = analysisRunId;
        this.userId = userId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    // LLM 호출을 시작하기 전에 먼저 기록한다 — 분석 응답 직후 상태 행 자체가 없는 레이스 컨디션을 최소화.
    public static RecommendationGeneration requested(Long analysisRunId, Long userId) {
        return new RecommendationGeneration(analysisRunId, userId, RecommendationGenerationStatus.REQUESTED, null, Instant.now(), null);
    }

    public RecommendationGeneration succeed() {
        return new RecommendationGeneration(analysisRunId, userId, RecommendationGenerationStatus.SUCCESS, null, createdAt, Instant.now());
    }

    public RecommendationGeneration fail(String errorMessage) {
        return new RecommendationGeneration(analysisRunId, userId, RecommendationGenerationStatus.FAILED, errorMessage, createdAt, Instant.now());
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static RecommendationGeneration of(Long analysisRunId, Long userId, RecommendationGenerationStatus status,
                                               String errorMessage, Instant createdAt, Instant completedAt) {
        return new RecommendationGeneration(analysisRunId, userId, status, errorMessage, createdAt, completedAt);
    }
}
