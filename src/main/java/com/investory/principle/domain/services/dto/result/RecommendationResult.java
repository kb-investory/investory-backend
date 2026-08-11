package com.investory.principle.domain.services.dto.result;

import com.investory.principle.domain.constant.RecommendationStatus;

public record RecommendationResult(
        Long recommendationId,
        String recommendationText,
        String recommendationReason,
        AnalysisTypeResult analysisType,
        RecommendationStatus recommendationStatus
) {
}
