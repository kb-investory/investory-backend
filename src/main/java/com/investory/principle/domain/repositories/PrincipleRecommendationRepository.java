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

    // 계정 탈퇴 시 — tendency가 analysis_results를 지우기 전에 넘겨주는 id 목록으로 그 결과를
    // 참조하는 추천들을 함께 지운다.
    void deleteByAnalysisResultIds(List<Long> analysisResultIds);
}
