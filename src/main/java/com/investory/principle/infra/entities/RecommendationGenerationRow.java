package com.investory.principle.infra.entities;

import com.investory.principle.domain.constant.RecommendationGenerationStatus;
import com.investory.principle.domain.model.RecommendationGeneration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class RecommendationGenerationRow {
    private Long analysisRunId;
    private Long userId;
    private RecommendationGenerationStatus generationStatus;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;

    public RecommendationGeneration toDomain() {
        return RecommendationGeneration.of(analysisRunId, userId, generationStatus, errorMessage, createdAt, completedAt);
    }

    public static RecommendationGenerationRow from(RecommendationGeneration generation) {
        RecommendationGenerationRow row = new RecommendationGenerationRow();
        row.analysisRunId = generation.getAnalysisRunId();
        row.userId = generation.getUserId();
        row.generationStatus = generation.getStatus();
        row.errorMessage = generation.getErrorMessage();
        row.createdAt = generation.getCreatedAt();
        row.completedAt = generation.getCompletedAt();
        return row;
    }
}
