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
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TendencyAnalyzedEventListenerTest {

    private FakePrincipleRecommendationRepository principleRecommendationRepository;
    private TendencyAnalyzedEventListener listener;

    @BeforeEach
    void setUp() {
        principleRecommendationRepository = new FakePrincipleRecommendationRepository();
        FakeRecommendationGenerationPort recommendationGenerationPort = new FakeRecommendationGenerationPort();
        recommendationGenerationPort.setNextResult(List.of(new GeneratedRecommendation("text", "reason", null)));
        Executor directExecutor = Runnable::run; // 테스트에서는 호출 스레드에서 그대로 동기 실행
        PrincipleService principleService = new PrincipleService(
                new FakePrincipleSetRepository(), principleRecommendationRepository, new FakeTendencyAnalysisPort(),
                recommendationGenerationPort, directExecutor);
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

    // 항목마다 따로 저장하면 늦게 끝난 항목의 추천만 뒤늦게 나타나는 시간차가 생긴다 — 그래서
    // saveAll이 실행(run) 1건당 정확히 한 번만 호출돼야 같은 실행의 추천들이 한꺼번에 나타난다.
    @Test
    void 실행_1건에_속한_추천은_saveAll_한번으로_한꺼번에_저장된다() {
        listener.handle(new TendencyAnalyzedEvent(100L, 1L, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"),
                new TendencyAnalyzedEvent.AnalysisResult(20L, "LOSS_RESPONSE", "ADDITIONAL_BUY", "추가매수형"))));

        assertEquals(1, principleRecommendationRepository.saveAllCallCount());
    }

    // 이 테스트는 listener.handle()을 직접 호출해서 @Async가 실제로 비동기 실행을 만드는지는
    // 검증하지 못한다(Spring 프록시를 안 거치므로). 대신 애너테이션 자체가 남아있는지만 지켜서,
    // 누군가 무심코 지웠을 때 POST /tendency/analyses가 다시 이 리스너의 LLM 호출을 물고
    // 504로 되돌아가는 회귀를 잡는다 — 실제 원인이었던 문제라 회귀 감지 가치가 크다.
    @Test
    void handle은_Async로_응답_경로에서_분리되어_있다() throws NoSuchMethodException {
        Method handle = TendencyAnalyzedEventListener.class.getMethod("handle", TendencyAnalyzedEvent.class);
        assertNotNull(handle.getAnnotation(Async.class));
    }
}
