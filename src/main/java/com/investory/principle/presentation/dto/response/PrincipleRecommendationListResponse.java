package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.RecommendationListResult;

import java.util.List;
import java.util.stream.Collectors;

public record PrincipleRecommendationListResponse(
        Long analysisRunId,
        String generationStatus,
        List<PrincipleRecommendationResponse> recommendations
) {
    public static PrincipleRecommendationListResponse from(RecommendationListResult result) {
        List<PrincipleRecommendationResponse> recommendations = result.recommendations().stream()
                .map(PrincipleRecommendationResponse::from)
                .collect(Collectors.toList());
        String generationStatus = result.generationStatus() == null ? null : result.generationStatus().name();
        return new PrincipleRecommendationListResponse(result.analysisRunId(), generationStatus, recommendations);
    }
}
