package com.investory.principle.domain.services.dto.result;

import com.investory.principle.domain.constant.RecommendationGenerationStatus;

import java.util.List;

public record RecommendationListResult(
        Long analysisRunId,
        RecommendationGenerationStatus generationStatus,
        List<RecommendationResult> recommendations
) {
}
