package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.RecommendationListResult;

import java.util.List;
import java.util.stream.Collectors;

public record PrincipleRecommendationListResponse(
        List<PrincipleRecommendationResponse> recommendations
) {
    public static PrincipleRecommendationListResponse from(RecommendationListResult result) {
        List<PrincipleRecommendationResponse> recommendations = result.recommendations().stream()
                .map(PrincipleRecommendationResponse::from)
                .collect(Collectors.toList());
        return new PrincipleRecommendationListResponse(recommendations);
    }
}
