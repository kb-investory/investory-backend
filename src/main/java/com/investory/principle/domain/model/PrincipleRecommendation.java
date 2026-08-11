package com.investory.principle.domain.model;

import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.exception.PrincipleErrorCode;
import com.investory.principle.domain.exception.PrincipleException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class PrincipleRecommendation {

    private final Long principleRecommendationId;
    private final Long analysisResultId;
    private final String recommendationText;
    private final String recommendationReason;
    private final String ruleJson;
    private final RecommendationStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PrincipleRecommendation(Long principleRecommendationId, Long analysisResultId, String recommendationText,
                                     String recommendationReason, String ruleJson, RecommendationStatus status,
                                     Instant createdAt, Instant updatedAt) {
        if (analysisResultId == null || recommendationText == null || recommendationReason == null || status == null) {
            throw new PrincipleException(PrincipleErrorCode.INVALID_PRINCIPLE_DATA);
        }

        this.principleRecommendationId = principleRecommendationId;
        this.analysisResultId = analysisResultId;
        this.recommendationText = recommendationText;
        this.recommendationReason = recommendationReason;
        this.ruleJson = ruleJson;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 신규 추천 후보 생성: 항상 SUGGESTED로 시작한다.
    public static PrincipleRecommendation create(Long analysisResultId, String recommendationText, String recommendationReason, String ruleJson) {
        Instant now = Instant.now();
        return new PrincipleRecommendation(null, analysisResultId, recommendationText, recommendationReason, ruleJson,
                RecommendationStatus.SUGGESTED, now, now);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static PrincipleRecommendation of(Long principleRecommendationId, Long analysisResultId, String recommendationText,
                                              String recommendationReason, String ruleJson, RecommendationStatus status,
                                              Instant createdAt, Instant updatedAt) {
        return new PrincipleRecommendation(principleRecommendationId, analysisResultId, recommendationText, recommendationReason,
                ruleJson, status, createdAt, updatedAt);
    }

    // DISMISSED된 추천은 사용자가 다시 볼 수 없어야 하므로 채택 대상이 될 수 없다 (ERD 상태 전이 규칙).
    public void validateAdoptable() {
        if (status == RecommendationStatus.DISMISSED) {
            throw new PrincipleException(PrincipleErrorCode.PRINCIPLE_CONFLICT);
        }
    }
}
