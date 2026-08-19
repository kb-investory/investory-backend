package com.investory.principle.domain.repositories;

import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.model.PrincipleRecommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakePrincipleRecommendationRepository implements PrincipleRecommendationRepository {

    private final List<PrincipleRecommendation> recommendations = new ArrayList<>();
    private long nextId = 1L;
    private int saveAllCallCount = 0;

    public void add(PrincipleRecommendation... recommendations) {
        this.recommendations.addAll(List.of(recommendations));
    }

    @Override
    public List<PrincipleRecommendation> findByAnalysisResultId(Long analysisResultId) {
        return recommendations.stream()
                .filter(r -> r.getAnalysisResultId().equals(analysisResultId))
                .collect(Collectors.toList());
    }

    @Override
    public List<PrincipleRecommendation> findByIds(List<Long> principleRecommendationIds) {
        return recommendations.stream()
                .filter(r -> principleRecommendationIds.contains(r.getPrincipleRecommendationId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PrincipleRecommendation> saveAll(List<PrincipleRecommendation> newRecommendations) {
        saveAllCallCount++;
        List<PrincipleRecommendation> saved = new ArrayList<>();
        for (PrincipleRecommendation r : newRecommendations) {
            PrincipleRecommendation withId = PrincipleRecommendation.of(nextId++, r.getAnalysisResultId(), r.getRecommendationText(),
                    r.getRecommendationReason(), r.getRuleJson(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
            recommendations.add(withId);
            saved.add(withId);
        }
        return saved;
    }

    public int saveAllCallCount() {
        return saveAllCallCount;
    }

    @Override
    public void updateStatus(Long principleRecommendationId, RecommendationStatus status) {
        for (int i = 0; i < recommendations.size(); i++) {
            PrincipleRecommendation r = recommendations.get(i);
            if (r.getPrincipleRecommendationId().equals(principleRecommendationId)) {
                recommendations.set(i, PrincipleRecommendation.of(r.getPrincipleRecommendationId(), r.getAnalysisResultId(),
                        r.getRecommendationText(), r.getRecommendationReason(), r.getRuleJson(), status, r.getCreatedAt(), r.getUpdatedAt()));
            }
        }
    }

    @Override
    public void deleteByAnalysisResultIds(List<Long> analysisResultIds) {
        recommendations.removeIf(r -> analysisResultIds.contains(r.getAnalysisResultId()));
    }
}
