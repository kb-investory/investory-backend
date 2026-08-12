package com.investory.tendency.domain.services;

import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import com.investory.tendency.domain.ports.FakeHoldingSummaryPort;
import com.investory.tendency.domain.ports.FakeMarketDataPort;
import com.investory.tendency.domain.ports.FakePrinciplePort;
import com.investory.tendency.domain.ports.FakeRationaleLabelStatsPort;
import com.investory.tendency.domain.ports.FakeTradeLedgerPort;
import com.investory.tendency.domain.ports.FakeTradeMatchQueryPort;
import com.investory.tendency.domain.repositories.FakeAnalysisResultRepository;
import com.investory.tendency.domain.repositories.FakeAnalysisRunRepository;
import com.investory.tendency.domain.services.dto.command.RunAnalysisCommand;
import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;
import com.investory.tendency.infra.clients.FakePrincipleComplianceGrader;
import com.investory.tendency.infra.clients.FakePrincipleRuleClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// runAnalysis()가 결과 저장 후 TendencyAnalyzedEvent를 실제로 발행하는지 확인하는 게 이 테스트의 핵심.
// 6개 항목 계산 자체의 정확성은 각 서비스 자신의 테스트(PrincipleAdherenceAnalysisServiceTest 등)가
// 이미 검증하므로, 여기선 전부 빈 데이터로 둬서 원칙 이행(6번, 판정불가형으로 항상 성공)만 결과에
// 남게 하고 나머지는 데이터 부족으로 스킵되는 경로를 그대로 탄다.
class AnalysisRunServiceTest {

    private static final Long USER_ID = 1L;

    private FakeAnalysisRunRepository analysisRunRepository;
    private FakeAnalysisResultRepository analysisResultRepository;
    private CapturingEventPublisher eventPublisher;
    private AnalysisRunService analysisRunService;

    @BeforeEach
    void setUp() {
        analysisRunRepository = new FakeAnalysisRunRepository();
        analysisResultRepository = new FakeAnalysisResultRepository();
        eventPublisher = new CapturingEventPublisher();

        PortfolioRiskAnalysisService portfolioRiskAnalysisService =
                new PortfolioRiskAnalysisService(new FakeHoldingSummaryPort(), new FakeMarketDataPort());
        RationaleTendencyService rationaleTendencyService = new RationaleTendencyService(new FakeRationaleLabelStatsPort());
        HoldingPeriodAnalysisService holdingPeriodAnalysisService = new HoldingPeriodAnalysisService(new FakeTradeMatchQueryPort());
        PrincipleAdherenceAnalysisService principleAdherenceAnalysisService = new PrincipleAdherenceAnalysisService(
                new FakePrinciplePort(), new FakeTradeLedgerPort(), new FakeMarketDataPort(),
                new FakePrincipleRuleClassifier(), new FakePrincipleComplianceGrader());

        analysisRunService = new AnalysisRunService(analysisRunRepository, analysisResultRepository,
                portfolioRiskAnalysisService, rationaleTendencyService, holdingPeriodAnalysisService,
                principleAdherenceAnalysisService, new FakeTradeLedgerPort(), new FakeMarketDataPort(), eventPublisher);
    }

    @Test
    void 분석_실행_후_결과가_저장된_만큼_이벤트가_발행된다() {
        AnalysisRunDetailResult result = analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));

        assertEquals(1, eventPublisher.events.size());
        TendencyAnalyzedEvent event = eventPublisher.events.get(0);
        assertEquals(USER_ID, event.userId());
        assertEquals(result.run().analysisRunId(), event.analysisRunId());
        assertEquals(1, event.results().size()); // 데이터가 전부 없어 원칙 이행(판정불가형)만 결과로 남음
        assertEquals("PRINCIPLE_ADHERENCE", event.results().get(0).analysisDimensionCode());
    }

    @Test
    void 이벤트의_결과ID는_실제_저장된_analysis_result_id와_일치한다() {
        analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));

        TendencyAnalyzedEvent event = eventPublisher.events.get(0);
        Long savedResultId = analysisResultRepository.findDetailByAnalysisRunId(event.analysisRunId()).get(0).analysisResultId();
        assertEquals(savedResultId, event.results().get(0).analysisResultId());
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final List<TendencyAnalyzedEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof TendencyAnalyzedEvent tendencyEvent) {
                events.add(tendencyEvent);
            }
        }
    }
}
