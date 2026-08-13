package com.investory.principle.infra.listeners;

import com.investory.principle.domain.model.PrincipleRecommendation;
import com.investory.principle.domain.ports.FakeRecommendationGenerationPort;
import com.investory.principle.domain.ports.FakeTendencyAnalysisPort;
import com.investory.principle.domain.ports.dto.GeneratedRecommendation;
import com.investory.principle.domain.repositories.FakePrincipleRecommendationRepository;
import com.investory.principle.domain.repositories.FakePrincipleSetRepository;
import com.investory.principle.domain.services.PrincipleService;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TendencyAnalyzedEventListenerTest {

    private FakePrincipleRecommendationRepository principleRecommendationRepository;
    private TendencyAnalyzedEventListener listener;

    @BeforeEach
    void setUp() {
        principleRecommendationRepository = new FakePrincipleRecommendationRepository();
        FakeRecommendationGenerationPort recommendationGenerationPort = new FakeRecommendationGenerationPort();
        recommendationGenerationPort.setNextResult(List.of(new GeneratedRecommendation("text", "reason", null)));
        PrincipleService principleService = new PrincipleService(
                new FakePrincipleSetRepository(), principleRecommendationRepository, new FakeTendencyAnalysisPort(),
                recommendationGenerationPort);
        listener = new TendencyAnalyzedEventListener(principleService);
    }

    @Test
    void 이벤트를_받으면_해당_분석결과에_대한_추천을_생성한다() {
        listener.handle(new TendencyAnalyzedEvent(100L, 1L, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"))));

        List<PrincipleRecommendation> saved = principleRecommendationRepository.findByAnalysisResultId(10L);
        assertTrue(saved.size() > 0);
    }

    @Test
    void 실행_1건에_속한_항목별_결과마다_각각_추천을_생성한다() {
        listener.handle(new TendencyAnalyzedEvent(100L, 1L, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"),
                new TendencyAnalyzedEvent.AnalysisResult(20L, "LOSS_RESPONSE", "ADDITIONAL_BUY", "추가매수형"))));

        assertTrue(principleRecommendationRepository.findByAnalysisResultId(10L).size() > 0);
        assertTrue(principleRecommendationRepository.findByAnalysisResultId(20L).size() > 0);
    }
}
