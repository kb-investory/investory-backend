package com.investory.tendency.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakePrincipleRecommendationCleanupPort implements PrincipleRecommendationCleanupPort {

    private final List<List<Long>> deleteCalls = new ArrayList<>();

    @Override
    public void deleteRecommendationsForAnalysisResults(List<Long> analysisResultIds) {
        deleteCalls.add(analysisResultIds);
    }

    public List<List<Long>> deleteCalls() {
        return deleteCalls;
    }
}
