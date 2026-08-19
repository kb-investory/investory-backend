package com.investory.principle.domain.repositories;

import com.investory.principle.domain.model.RecommendationGeneration;

import java.util.Optional;

public interface RecommendationGenerationRepository {
    Optional<RecommendationGeneration> findByAnalysisRunId(Long analysisRunId);

    // analysisRunId 기준 upsert — REQUESTED로 최초 기록한 뒤 SUCCESS/FAILED로 같은 행을 갱신한다.
    void save(RecommendationGeneration generation);
}
