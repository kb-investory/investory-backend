package com.investory.tendency.domain.services;

import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import com.investory.tendency.domain.ports.FakeHoldingSummaryPort;
import com.investory.tendency.domain.ports.FakeJournalRationalePort;
import com.investory.tendency.domain.ports.FakeMarketDataPort;
import com.investory.tendency.domain.ports.FakePrincipleRecommendationCleanupPort;
import com.investory.tendency.domain.ports.FakePrinciplePort;
import com.investory.tendency.domain.ports.FakeRationaleLabelStatsPort;
import com.investory.tendency.domain.ports.FakeTradeLedgerPort;
import com.investory.tendency.domain.ports.FakeTradeMatchQueryPort;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import com.investory.tendency.domain.repositories.FakeAnalysisResultRepository;
import com.investory.tendency.domain.repositories.FakeAnalysisRunRepository;
import com.investory.tendency.domain.services.dto.command.RunAnalysisCommand;
import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;
import com.investory.tendency.infra.clients.FakePrincipleComplianceGrader;
import com.investory.tendency.infra.clients.FakePrincipleRuleClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

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
    private FakePrincipleRecommendationCleanupPort principleRecommendationCleanupPort;
    private FakeJournalRationalePort journalRationalePort;
    private FakeTradeLedgerPort tradeLedgerPort;
    private FakeMarketDataPort marketDataPort;
    private AnalysisRunService analysisRunService;

    @BeforeEach
    void setUp() {
        analysisRunRepository = new FakeAnalysisRunRepository();
        analysisResultRepository = new FakeAnalysisResultRepository();
        eventPublisher = new CapturingEventPublisher();
        principleRecommendationCleanupPort = new FakePrincipleRecommendationCleanupPort();
        journalRationalePort = new FakeJournalRationalePort();
        tradeLedgerPort = new FakeTradeLedgerPort();
        marketDataPort = new FakeMarketDataPort();

        PortfolioRiskAnalysisService portfolioRiskAnalysisService =
                new PortfolioRiskAnalysisService(new FakeHoldingSummaryPort(), new FakeMarketDataPort());
        RationaleTendencyService rationaleTendencyService = new RationaleTendencyService(new FakeRationaleLabelStatsPort());
        HoldingPeriodAnalysisService holdingPeriodAnalysisService = new HoldingPeriodAnalysisService(new FakeTradeMatchQueryPort());
        Executor directExecutor = Runnable::run; // 테스트에서는 호출 스레드에서 그대로 동기 실행
        PrincipleAdherenceAnalysisService principleAdherenceAnalysisService = new PrincipleAdherenceAnalysisService(
                new FakePrinciplePort(), new FakeTradeLedgerPort(), new FakeMarketDataPort(),
                new FakePrincipleRuleClassifier(), new FakePrincipleComplianceGrader(), directExecutor);

        analysisRunService = new AnalysisRunService(analysisRunRepository, analysisResultRepository,
                portfolioRiskAnalysisService, rationaleTendencyService, holdingPeriodAnalysisService,
                principleAdherenceAnalysisService, tradeLedgerPort, marketDataPort,
                journalRationalePort, eventPublisher, principleRecommendationCleanupPort);
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
    void journalRationalePort가_반환한_건수가_저장된_run의_journalCount로_채워진다() {
        journalRationalePort.setCount(7);

        AnalysisRunDetailResult result = analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));

        assertEquals(7, result.run().journalCount());
    }

    @Test
    void 이벤트의_결과ID는_실제_저장된_analysis_result_id와_일치한다() {
        analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));

        TendencyAnalyzedEvent event = eventPublisher.events.get(0);
        Long savedResultId = analysisResultRepository.findDetailByAnalysisRunId(event.analysisRunId()).get(0).analysisResultId();
        assertEquals(savedResultId, event.results().get(0).analysisResultId());
    }

    @Test
    void 계정_탈퇴시_사용자의_성향분석_기록을_전부_지우고_그_전에_principle에_결과ID_목록을_알려준다() {
        com.investory.tendency.domain.model.AnalysisRun run = analysisRunRepository.save(
                com.investory.tendency.domain.model.AnalysisRun.create(USER_ID, java.time.LocalDate.now().minusDays(89),
                        java.time.LocalDate.now(), 0, 0, "1.0"));
        analysisResultRepository.add(USER_ID, com.investory.tendency.domain.model.AnalysisResult.create(
                run.getAnalysisRunId(), "PORTFOLIO_RISK_ALLOCATION", "RISK_CONCENTRATED", "{}"));

        analysisRunService.deleteAllAnalyses(USER_ID);

        assertTrue(analysisRunRepository.findByUserId(USER_ID).isEmpty());
        assertTrue(analysisResultRepository.findIdsByUserId(USER_ID).isEmpty());
        assertEquals(1, principleRecommendationCleanupPort.deleteCalls().size());
    }

    // #172 회귀 테스트 — 종목 하나가 (종목,일자) 조합 수를 독점해도 손실 대응 라벨이 그 종목에
    // 끌려가지 않고, 종목 단위 다수결을 따라야 한다.
    // 종목101: 매수 후 계속 보유만 하며 9일 연속 손실 상태(HOLD 9표) — 예전 방식이면 이 날짜 수가
    //          그대로 전체 합산에 더해져 압도적 다수를 차지했다.
    // 종목102·103: 매수 다음날 바로 손절매도(각 1일, STOP_LOSS 1표씩).
    // 예전(합산) 방식: totalDays=11, holdDays=9 → 9/11≈82% → HOLD로 오판정.
    // 수정 후(종목별 선판정 후 다수결) 방식: 종목 3개 중 STOP_LOSS가 2표(2/3≈67%) → STOP_LOSS.
    @Test
    void 손실_대응_성향은_날짜_합산이_아니라_종목_단위_다수결로_판정한다() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        tradeLedgerPort.add(buyTrade(101L, today.minusDays(10), 10, 100));
        for (long d = 9; d >= 1; d--) {
            marketDataPort.addPrice(101L, today.minusDays(d), 90.0);
        }

        tradeLedgerPort.add(buyTrade(102L, today.minusDays(5), 10, 100));
        tradeLedgerPort.add(sellTrade(102L, today.minusDays(4), 10, 90));

        tradeLedgerPort.add(buyTrade(103L, today.minusDays(5), 10, 100));
        tradeLedgerPort.add(sellTrade(103L, today.minusDays(4), 10, 90));

        analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));

        TendencyAnalyzedEvent event = eventPublisher.events.get(0);
        String lossTypeCode = event.results().stream()
                .filter(r -> r.analysisDimensionCode().equals("LOSS_RESPONSE"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("LOSS_RESPONSE 결과가 없음"))
                .analysisTypeCode();

        assertEquals("LOSS_STOP_LOSS", lossTypeCode);
    }

    private TradeInfo buyTrade(Long securityId, LocalDate date, int quantity, double price) {
        return new TradeInfo(securityId, "BUY", BigDecimal.valueOf(quantity), BigDecimal.valueOf(price), toInstant(date));
    }

    private TradeInfo sellTrade(Long securityId, LocalDate date, int quantity, double price) {
        return new TradeInfo(securityId, "SELL", BigDecimal.valueOf(quantity), BigDecimal.valueOf(price), toInstant(date));
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
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
