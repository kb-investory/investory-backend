package com.investory.principle.infra.port_impls;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.tendency.domain.ports.PrincipleRecommendationCleanupPort;
import org.springframework.stereotype.Component;

import java.util.List;

// tendency.domain.ports를 참조하는 유일한 지점 — 받는 즉시 principle 자신의 서비스 호출로 위임한다(§5).
@Component
public class PrincipleRecommendationCleanupPortImpl implements PrincipleRecommendationCleanupPort {

    private final PrincipleService principleService;

    public PrincipleRecommendationCleanupPortImpl(PrincipleService principleService) {
        this.principleService = principleService;
    }

    @Override
    public void deleteRecommendationsForAnalysisResults(List<Long> analysisResultIds) {
        principleService.deleteRecommendationsForAnalysisResults(analysisResultIds);
    }
}
