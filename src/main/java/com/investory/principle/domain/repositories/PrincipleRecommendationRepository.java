package com.investory.principle.domain.repositories;

import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.model.PrincipleRecommendation;

import java.util.List;

public interface PrincipleRecommendationRepository {
    List<PrincipleRecommendation> findByAnalysisResultId(Long analysisResultId);
    List<PrincipleRecommendation> findByIds(List<Long> principleRecommendationIds);

    // 신규 추천 후보를 insert하고, 생성된 PK가 채워진 상태로 반환한다.
    List<PrincipleRecommendation> saveAll(List<PrincipleRecommendation> recommendations);

    void updateStatus(Long principleRecommendationId, RecommendationStatus status);
}
