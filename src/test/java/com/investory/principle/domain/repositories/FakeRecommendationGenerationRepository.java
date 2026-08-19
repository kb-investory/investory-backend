package com.investory.principle.domain.repositories;

import com.investory.principle.domain.model.RecommendationGeneration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeRecommendationGenerationRepository implements RecommendationGenerationRepository {

    private final Map<Long, RecommendationGeneration> generationsByAnalysisRunId = new HashMap<>();

    @Override
    public Optional<RecommendationGeneration> findByAnalysisRunId(Long analysisRunId) {
        return Optional.ofNullable(generationsByAnalysisRunId.get(analysisRunId));
    }

    @Override
    public void save(RecommendationGeneration generation) {
        generationsByAnalysisRunId.put(generation.getAnalysisRunId(), generation);
    }
}
