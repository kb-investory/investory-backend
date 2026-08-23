package com.investory.principle.infra.listeners;

import com.investory.principle.domain.model.PrincipleRecommendation;
import com.investory.principle.domain.ports.FakeRecommendationGenerationPort;
import com.investory.principle.domain.ports.FakeTendencyAnalysisPort;
import com.investory.principle.domain.ports.dto.GeneratedRecommendation;
import com.investory.principle.domain.repositories.FakePrincipleRecommendationRepository;
import com.investory.principle.domain.repositories.FakePrincipleSetRepository;
import com.investory.principle.domain.repositories.FakeRecommendationGenerationRepository;
import com.investory.principle.domain.services.PrincipleService;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TendencyAnalyzedEventListenerTest {

    private FakePrincipleRecommendationRepository principleRecommendationRepository;
    private PrincipleService principleService;
    private TendencyAnalyzedEventListener listener;

    @BeforeEach
    void setUp() {
        principleRecommendationRepository = new FakePrincipleRecommendationRepository();
        FakeRecommendationGenerationPort recommendationGenerationPort = new FakeRecommendationGenerationPort();
        recommendationGenerationPort.setNextResult(List.of(new GeneratedRecommendation("text", "reason", null)));
        Executor directExecutor = Runnable::run; // 테스트에서는 호출 스레드에서 그대로 동기 실행
        principleService = new PrincipleService(
                new FakePrincipleSetRepository(), principleRecommendationRepository, new FakeRecommendationGenerationRepository(),
                new FakeTendencyAnalysisPort(), recommendationGenerationPort, directExecutor);
        listener = new TendencyAnalyzedEventListener(principleService, directExecutor);
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

    // handle()이 추천 생성을 호출 스레드에서 직접 하지 않고 실행기에 제출만 하는지 확인한다 —
    // 실행하지 않는(제출만 기록하는) 실행기를 주면, handle()이 정상 반환된 시점엔 아직 아무것도
    // 저장돼 있지 않아야 한다. 누군가 무심코 실행기 제출을 없애고 직접 호출로 되돌리면
    // POST /tendency/analyses가 다시 이 리스너의 LLM 호출을 물고 504로 되돌아가는 회귀를 잡는다.
    @Test
    void handle은_추천_생성을_직접_실행하지_않고_실행기에_제출만_한다() {
        AtomicBoolean submitted = new AtomicBoolean(false);
        Executor recordingExecutor = command -> submitted.set(true); // 제출만 기록, 실행은 안 함
        TendencyAnalyzedEventListener recordingListener = new TendencyAnalyzedEventListener(principleService, recordingExecutor);

        recordingListener.handle(new TendencyAnalyzedEvent(100L, 1L, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"))));

        assertTrue(submitted.get());
        assertTrue(principleRecommendationRepository.findByAnalysisResultId(10L).isEmpty());
    }

    // #204 회귀 가드 — 1000 VU 부하테스트에서 tendencyLlmExecutor/principleRecommendationExecutor
    // 큐가 가득 차면 CompletableFuture 제출 자체가 RejectedExecutionException을 던진다. 예전엔
    // @Async 프록시 단계에서 이 예외가 곧장 던져져 publishEvent() -> runAnalysis()까지 전파되며
    // 이미 저장된 분석 결과를 500으로 되돌렸다. handle()은 이 거부를 흡수하고 예외 없이 반환해야 한다.
    @Test
    void 실행기_제출이_거부돼도_예외가_전파되지_않는다() {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("simulated queue saturation");
        };
        TendencyAnalyzedEventListener rejectingListener = new TendencyAnalyzedEventListener(principleService, rejectingExecutor);

        rejectingListener.handle(new TendencyAnalyzedEvent(100L, 1L, List.of(
                new TendencyAnalyzedEvent.AnalysisResult(10L, "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "집중투자형"))));
    }
}
