package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.RecommendationResult;

public record PrincipleRecommendationResponse(
        Long recommendationId,
        String recommendationText,
        String recommendationReason,
        AnalysisTypeResponse analysisType,
        String recommendationStatus
) {
    public static PrincipleRecommendationResponse from(RecommendationResult result) {
        return new PrincipleRecommendationResponse(
                result.recommendationId(),
                result.recommendationText(),
                result.recommendationReason(),
                AnalysisTypeResponse.from(result.analysisType()),
                result.recommendationStatus().name());
    }
}
