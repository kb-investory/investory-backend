package com.investory.tendency.infra.port_impls;

import com.investory.auth.domain.ports.TendencyCleanupPort;
import com.investory.tendency.domain.services.AnalysisRunService;
import org.springframework.stereotype.Component;

// auth.domain.ports를 참조하는 유일한 지점 — 받는 즉시 tendency 자신의 서비스 호출로 위임한다(§5).
@Component
public class TendencyCleanupPortImpl implements TendencyCleanupPort {

    private final AnalysisRunService analysisRunService;

    public TendencyCleanupPortImpl(AnalysisRunService analysisRunService) {
        this.analysisRunService = analysisRunService;
    }

    @Override
    public void deleteAllAnalyses(Long userId) {
        analysisRunService.deleteAllAnalyses(userId);
    }
}
