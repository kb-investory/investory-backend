package com.investory.principle.domain.ports;

import com.investory.principle.domain.ports.dto.GeneratedRecommendation;
import com.investory.principle.infra.exception.RecommendationGenerationException;

import java.util.List;

public class FakeRecommendationGenerationPort implements RecommendationGenerationPort {

    private List<GeneratedRecommendation> nextResult = List.of();
    private boolean shouldFail = false;

    public void setNextResult(List<GeneratedRecommendation> nextResult) {
        this.nextResult = nextResult;
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public List<GeneratedRecommendation> generate(String analysisTypeCode, String analysisTypeName) {
        if (shouldFail) {
            throw new RecommendationGenerationException(new RuntimeException("test failure"));
        }
        return nextResult;
    }
}
