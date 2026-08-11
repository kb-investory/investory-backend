package com.investory.principle.infra.entities;

import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.model.PrincipleRecommendation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class PrincipleRecommendationRow {
    private Long principleRecommendationId;
    private Long analysisResultId;
    private String recommendationText;
    private String recommendationReason;
    private String ruleJson;
    private RecommendationStatus recommendationStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public PrincipleRecommendation toDomain() {
        return PrincipleRecommendation.of(principleRecommendationId, analysisResultId, recommendationText, recommendationReason,
                ruleJson, recommendationStatus, createdAt, updatedAt);
    }

    public static PrincipleRecommendationRow from(PrincipleRecommendation recommendation) {
        PrincipleRecommendationRow row = new PrincipleRecommendationRow();
        row.principleRecommendationId = recommendation.getPrincipleRecommendationId();
        row.analysisResultId = recommendation.getAnalysisResultId();
        row.recommendationText = recommendation.getRecommendationText();
        row.recommendationReason = recommendation.getRecommendationReason();
        row.ruleJson = recommendation.getRuleJson();
        row.recommendationStatus = recommendation.getStatus();
        row.createdAt = recommendation.getCreatedAt();
        row.updatedAt = recommendation.getUpdatedAt();
        return row;
    }
}
