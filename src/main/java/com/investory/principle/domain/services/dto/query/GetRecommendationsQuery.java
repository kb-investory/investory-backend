package com.investory.principle.domain.services.dto.query;

// analysisRunId는 선택 — null이면 기존과 동일하게 generationStatus 없이 추천 목록만 반환한다.
public record GetRecommendationsQuery(Long userId, Long analysisRunId) {
}
