package com.investory.tendency.domain.services;

import com.investory.tendency.domain.constant.AnalysisRunStatus;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
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
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunDetailQuery;
import com.investory.tendency.domain.services.dto.result.AnalysisRunAcceptedResult;
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
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        // 직접 실행기라 runAnalysis() 안의 CompletableFuture.runAsync()가 호출 스레드에서 그대로
        // 동기 실행된다 — runAnalysis()가 반환된 시점엔 executeAnalysis()까지 이미 다 끝나 있다.
        analysisRunService = buildService(Runnable::run);
    }

    // 같은 fake 저장소/포트를 재사용하되 실행기만 바꿔서 서비스를 새로 만든다 — 큐 포화/거부 테스트처럼
    // 다른 실행기 동작이 필요한 케이스에서 setUp()의 배선을 중복하지 않기 위함.
    private AnalysisRunService buildService(Executor executor) {
        PortfolioRiskAnalysisService portfolioRiskAnalysisService =
                new PortfolioRiskAnalysisService(new FakeHoldingSummaryPort(), new FakeMarketDataPort());
        RationaleTendencyService rationaleTendencyService = new RationaleTendencyService(new FakeRationaleLabelStatsPort());
        HoldingPeriodAnalysisService holdingPeriodAnalysisService = new HoldingPeriodAnalysisService(new FakeTradeMatchQueryPort());
        PrincipleAdherenceAnalysisService principleAdherenceAnalysisService = new PrincipleAdherenceAnalysisService(
                new FakePrinciplePort(), new FakeTradeLedgerPort(), new FakeMarketDataPort(),
                new FakePrincipleRuleClassifier(), new FakePrincipleComplianceGrader(), Runnable::run);

        return new AnalysisRunService(analysisRunRepository, analysisResultRepository,
                portfolioRiskAnalysisService, rationaleTendencyService, holdingPeriodAnalysisService,
                principleAdherenceAnalysisService, tradeLedgerPort, marketDataPort,
                journalRationalePort, eventPublisher, principleRecommendationCleanupPort, executor);
    }

    @Test
    void 분석_실행_후_결과가_저장된_만큼_이벤트가_발행된다() {
        AnalysisRunAcceptedResult result = analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));

        assertEquals(1, eventPublisher.events.size());
        TendencyAnalyzedEvent event = eventPublisher.events.get(0);
        assertEquals(USER_ID, event.userId());
        assertEquals(result.analysisRunId(), event.analysisRunId());
        assertEquals(1, event.results().size()); // 데이터가 전부 없어 원칙 이행(판정불가형)만 결과로 남음
        assertEquals("PRINCIPLE_ADHERENCE", event.results().get(0).analysisDimensionCode());
    }

    @Test
    void journalRationalePort가_반환한_건수가_저장된_run의_journalCount로_채워진다() {
        journalRationalePort.setCount(7);

        AnalysisRunAcceptedResult accepted = analysisRunService.runAnalysis(new RunAnalysisCommand(USER_ID));
        AnalysisRunDetailResult detail = analysisRunService.getDetail(new GetAnalysisRunDetailQuery(USER_ID, accepted.analysisRunId()));

        assertEquals(7, detail.run().journalCount());
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
                        java.time.LocalDate.now(), "1.0"));
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

    // #207 회귀 테스트 — runAnalysis()는 DB 읽기 없이 REQUESTED 행만 만들고 즉시 반환해야 한다.
    // 제출만 기록하고 실제로 실행하지 않는 실행기를 써서, runAnalysis()가 반환된 시점엔 분석 작업이
    // 전혀 실행되지 않았음을 확인한다.
    @Test
    void 요청_경로는_분석_작업_없이_REQUESTED_상태의_실행만_즉시_생성한다() {
        List<Runnable> submitted = new ArrayList<>();
        Executor capturingExecutor = submitted::add; // 제출만 기록, 실행은 안 함
        AnalysisRunService capturingService = buildService(capturingExecutor);

        AnalysisRunAcceptedResult accepted = capturingService.runAnalysis(new RunAnalysisCommand(USER_ID));

        assertEquals(AnalysisRunStatus.REQUESTED, accepted.runStatus());
        assertTrue(analysisResultRepository.findDetailByAnalysisRunId(accepted.analysisRunId()).isEmpty());
        assertTrue(eventPublisher.events.isEmpty());
        assertEquals(1, submitted.size());

        submitted.get(0).run(); // 워커가 나중에 실제로 처리하는 상황을 흉내낸다
        AnalysisRunDetailResult detail = capturingService.getDetail(new GetAnalysisRunDetailQuery(USER_ID, accepted.analysisRunId()));
        assertEquals(AnalysisRunStatus.SUCCESS, detail.run().runStatus());
    }

    // #207 회귀 가드 — analysisRunExecutor 큐가 가득 차면 CompletableFuture.runAsync() 제출 자체가
    // RejectedExecutionException을 던진다. runAnalysis()는 이 예외를 흡수하고 즉시 FAILED로
    // 마감해야 한다(REQUESTED에 영원히 머무는 상태 금지).
    @Test
    void 실행기_제출이_거부되면_예외를_전파하지_않고_즉시_FAILED로_마감한다() {
        Executor alwaysRejectingExecutor = command -> {
            throw new RejectedExecutionException("simulated queue saturation");
        };
        AnalysisRunService rejectingService = buildService(alwaysRejectingExecutor);

        AnalysisRunAcceptedResult accepted = rejectingService.runAnalysis(new RunAnalysisCommand(USER_ID));

        assertEquals(AnalysisRunStatus.FAILED, accepted.runStatus());
        AnalysisRunDetailResult detail = rejectingService.getDetail(new GetAnalysisRunDetailQuery(USER_ID, accepted.analysisRunId()));
        assertEquals(AnalysisRunStatus.FAILED, detail.run().runStatus());
        assertFalse(detail.errorMessage().isBlank());
    }

    // 워커 처리 중 예외가 나도 FAILED + 에러메시지로 끝나야 한다(REQUESTED/RUNNING에 머무는 상태 금지).
    @Test
    void 작업_처리_중_예외가_발생하면_FAILED_상태와_에러메시지가_남는다() {
        FakeTradeLedgerPort throwingTradeLedgerPort = new FakeTradeLedgerPort() {
            @Override
            public List<TradeInfo> findAllTrades(Long userId) {
                throw new RuntimeException("거래 조회 실패(테스트)");
            }
        };
        AnalysisRunService failingService = new AnalysisRunService(analysisRunRepository, analysisResultRepository,
                new PortfolioRiskAnalysisService(new FakeHoldingSummaryPort(), new FakeMarketDataPort()),
                new RationaleTendencyService(new FakeRationaleLabelStatsPort()),
                new HoldingPeriodAnalysisService(new FakeTradeMatchQueryPort()),
                new PrincipleAdherenceAnalysisService(new FakePrinciplePort(), new FakeTradeLedgerPort(), new FakeMarketDataPort(),
                        new FakePrincipleRuleClassifier(), new FakePrincipleComplianceGrader(), Runnable::run),
                throwingTradeLedgerPort, marketDataPort, journalRationalePort, eventPublisher,
                principleRecommendationCleanupPort, Runnable::run);

        AnalysisRunAcceptedResult accepted = failingService.runAnalysis(new RunAnalysisCommand(USER_ID));

        AnalysisRunDetailResult detail = failingService.getDetail(new GetAnalysisRunDetailQuery(USER_ID, accepted.analysisRunId()));
        assertEquals(AnalysisRunStatus.FAILED, detail.run().runStatus());
        assertTrue(detail.errorMessage().contains("거래 조회 실패"));
    }

    // 중복 제출 가드 — 이미 REQUESTED/RUNNING 상태의 실행이 있으면 새 요청을 409로 거부한다.
    @Test
    void 이미_진행중인_분석이_있으면_새_요청을_거부한다() {
        List<Runnable> submitted = new ArrayList<>();
        AnalysisRunService pendingService = buildService(submitted::add); // 제출만 기록, 실행 안 해서 REQUESTED로 남김
        pendingService.runAnalysis(new RunAnalysisCommand(USER_ID));

        TendencyException e = assertThrows(TendencyException.class,
                () -> pendingService.runAnalysis(new RunAnalysisCommand(USER_ID)));
        assertEquals(TendencyErrorCode.ANALYSIS_ALREADY_IN_PROGRESS, e.getErrorCode());
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
