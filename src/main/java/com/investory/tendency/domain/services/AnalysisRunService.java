package com.investory.tendency.domain.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.tendency.domain.constant.AnalysisRunStatus;
import com.investory.tendency.domain.constant.GainResponseType;
import com.investory.tendency.domain.constant.LossResponseType;
import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;
import com.investory.tendency.domain.model.AnalysisRun;
import com.investory.tendency.domain.ports.JournalRationalePort;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.PrincipleRecommendationCleanupPort;
import com.investory.tendency.domain.ports.TradeLedgerPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import com.investory.tendency.domain.repositories.AnalysisResultRepository;
import com.investory.tendency.domain.repositories.AnalysisRunRepository;
import com.investory.tendency.domain.services.dto.command.RunAnalysisCommand;
import com.investory.tendency.domain.services.dto.query.AnalyzeHoldingPeriodQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzePortfolioRiskQuery;
import com.investory.tendency.domain.services.dto.command.AnalyzeRationaleTendencyQuery;
import com.investory.tendency.domain.services.dto.query.AnalyzePrincipleAdherenceQuery;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunDetailQuery;
import com.investory.tendency.domain.services.dto.query.GetAnalysisRunsQuery;
import com.investory.tendency.domain.services.dto.result.AnalysisItemResult;
import com.investory.tendency.domain.services.dto.result.AnalysisRunAcceptedResult;
import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;
import com.investory.tendency.domain.services.dto.result.AnalysisRunSummaryResult;
import com.investory.tendency.domain.services.dto.result.HoldingPeriodAnalysisResult;
import com.investory.tendency.domain.services.dto.result.PortfolioRiskAnalysisResult;
import com.investory.tendency.domain.services.dto.result.PrincipleAdherenceAnalysisResult;
import com.investory.tendency.domain.services.dto.result.RationaleTendencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

// 6개 항목을 한 번에 실행해서 analysis_runs/analysis_results에 저장하고, 이력/상세를 조회하는 오케스트레이션 서비스.
// 1·2·5·6번은 이미 포트폴리오 전체 기준으로 계산하는 기존 서비스를 그대로 재사용한다.
// 3·4번(손실/수익 대응)은 기존 서비스가 종목 단위(securityId)로만 계산하는데, analysis_results는
// 실행당 항목별 결과가 1행이어야 해서(UNIQUE(analysis_run_id, analysis_dimension_code)) 여기서
// 종목별로 먼저 라벨을 매긴 뒤, 그 라벨들을 종목 단위로 다시 다수결을 내 하나의 라벨로 합산한다.
// (종목,일자) 조합 수를 그대로 하나의 카운터에 합산하던 이전 방식은 매매가 잦거나 오래 물려있던
// 종목 하나가 표를 독점해버리는 문제가 있었다(#172).
@Service
public class AnalysisRunService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRunService.class);

    private static final int ANALYSIS_WINDOW_DAYS = 90;
    private static final String ANALYSIS_VERSION = "1.0";
    private static final BigDecimal LOSS_GAIN_THRESHOLD = BigDecimal.valueOf(60, 2);
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private static final String DIM_PORTFOLIO_RISK = "PORTFOLIO_RISK_ALLOCATION";
    private static final String DIM_BUY_DECISION_BASIS = "PURCHASE_RATIONALE";
    private static final String DIM_LOSS_RESPONSE = "LOSS_RESPONSE";
    private static final String DIM_PROFIT_RESPONSE = "PROFIT_RESPONSE";
    private static final String DIM_HOLDING_PERIOD = "HOLDING_PERIOD";
    private static final String DIM_PRINCIPLE_ADHERENCE = "PRINCIPLE_ADHERENCE";

    private final AnalysisRunRepository analysisRunRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final PortfolioRiskAnalysisService portfolioRiskAnalysisService;
    private final RationaleTendencyService rationaleTendencyService;
    private final HoldingPeriodAnalysisService holdingPeriodAnalysisService;
    private final PrincipleAdherenceAnalysisService principleAdherenceAnalysisService;
    private final TradeLedgerPort tradeLedgerPort;
    private final MarketDataPort marketDataPort;
    private final JournalRationalePort journalRationalePort;
    private final ApplicationEventPublisher eventPublisher;
    private final PrincipleRecommendationCleanupPort principleRecommendationCleanupPort;
    private final Executor analysisRunExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisRunService(AnalysisRunRepository analysisRunRepository,
                               AnalysisResultRepository analysisResultRepository,
                               PortfolioRiskAnalysisService portfolioRiskAnalysisService,
                               RationaleTendencyService rationaleTendencyService,
                               HoldingPeriodAnalysisService holdingPeriodAnalysisService,
                               PrincipleAdherenceAnalysisService principleAdherenceAnalysisService,
                               TradeLedgerPort tradeLedgerPort,
                               MarketDataPort marketDataPort,
                               JournalRationalePort journalRationalePort,
                               ApplicationEventPublisher eventPublisher,
                               PrincipleRecommendationCleanupPort principleRecommendationCleanupPort,
                               @Qualifier("analysisRunExecutor") Executor analysisRunExecutor) {
        this.analysisRunRepository = analysisRunRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.portfolioRiskAnalysisService = portfolioRiskAnalysisService;
        this.rationaleTendencyService = rationaleTendencyService;
        this.holdingPeriodAnalysisService = holdingPeriodAnalysisService;
        this.principleAdherenceAnalysisService = principleAdherenceAnalysisService;
        this.principleRecommendationCleanupPort = principleRecommendationCleanupPort;
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
        this.journalRationalePort = journalRationalePort;
        this.eventPublisher = eventPublisher;
        this.analysisRunExecutor = analysisRunExecutor;
    }

    // 요청 경로(#207) — POST /tendency/analyses가 직접 호출. DB 읽기 없이 REQUESTED 상태의 실행
    // 행 하나만 만들고 실제 분석(executeAnalysis)은 analysisRunExecutor에 제출한 뒤 즉시 반환한다.
    // 예전엔 이 메서드가 6개 항목 계산 + 결과 저장 + 이벤트 발행까지 요청 스레드에서 전부 동기로
    // 처리해 9~13초가 걸렸고, 이게 1000 VU 부하테스트에서 Tomcat/HikariCP 풀 고갈의 근본 원인으로
    // 확인됐다(GitHub #207). 이미 진행 중인 분석이 있으면 중복 제출을 막는다 — 그렇지 않으면 한
    // 사용자가 연달아 호출해 공유 실행기를 잠식할 수 있다.
    public AnalysisRunAcceptedResult runAnalysis(RunAnalysisCommand command) {
        Long userId = command.userId();
        if (analysisRunRepository.existsInProgressByUserId(userId)) {
            throw new TendencyException(TendencyErrorCode.ANALYSIS_ALREADY_IN_PROGRESS);
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(ANALYSIS_WINDOW_DAYS - 1L);
        AnalysisRun saved = analysisRunRepository.save(AnalysisRun.create(userId, windowStart, today, ANALYSIS_VERSION));

        try {
            CompletableFuture.runAsync(() -> executeAnalysis(saved.getAnalysisRunId(), userId, windowStart, today), analysisRunExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("성향분석 작업 제출 실패(큐 포화) — 즉시 FAILED로 마감합니다. analysisRunId={}", saved.getAnalysisRunId(), e);
            analysisRunRepository.markFailed(saved.getAnalysisRunId(), "분석 작업을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            return new AnalysisRunAcceptedResult(saved.getAnalysisRunId(), AnalysisRunStatus.FAILED);
        }
        return new AnalysisRunAcceptedResult(saved.getAnalysisRunId(), AnalysisRunStatus.REQUESTED);
    }

    // 실제 분석 본체 — analysisRunExecutor에서 실행된다. 반드시 SUCCESS 또는 FAILED로 끝나야 한다
    // (REQUESTED/RUNNING에 영원히 머무는 상태를 만들지 않는다, CLAUDE.md §11).
    private void executeAnalysis(Long analysisRunId, Long userId, LocalDate windowStart, LocalDate today) {
        Instant runStart = Instant.now();
        try {
            analysisRunRepository.markRunning(analysisRunId);

            List<TradeInfo> allTrades = tradeLedgerPort.findAllTrades(userId);

            List<AnalysisResult> results = new ArrayList<>();
            collectPortfolioRisk(userId, results);
            collectBuyDecisionBasis(userId, results);
            collectLossOrGain(allTrades, windowStart, today, true, results);
            collectLossOrGain(allTrades, windowStart, today, false, results);
            collectHoldingPeriod(userId, results);
            collectPrincipleAdherence(userId, results);

            int journalCount = journalRationalePort.countJournalsInRange(userId, windowStart, today);
            List<AnalysisResult> resultsWithRunId = results.stream()
                    .map(r -> AnalysisResult.create(analysisRunId, r.getAnalysisDimensionCode(), r.getPrimaryAnalysisTypeCode(), r.getEvidenceJson()))
                    .collect(Collectors.toList());
            analysisResultRepository.saveAll(resultsWithRunId);

            analysisRunRepository.markSuccess(analysisRunId, allTrades.size(), journalCount);

            // markSuccess 커밋 이후에 발행하고, 여기서 실패해도 SUCCESS를 FAILED로 되돌리지 않는다 —
            // 분석 자체는 이미 정상적으로 끝나 저장됐으므로.
            try {
                publishAnalyzedEvent(userId, analysisRunId);
            } catch (RuntimeException e) {
                log.warn("성향분석 완료 이벤트 발행 실패 — 분석 자체는 SUCCESS로 유지합니다. analysisRunId={}", analysisRunId, e);
            }

            // 프론트가 폴링으로 확인하게 되는 실제 분석 소요시간 — 6개 항목 계산을 다 합친 총 소요시간.
            log.info("성향분석 실행 완료. userId={}, analysisRunId={}, tradeCount={}, totalDurationMs={}",
                    userId, analysisRunId, allTrades.size(), Duration.between(runStart, Instant.now()).toMillis());
        } catch (Exception e) {
            log.error("성향분석 실행 실패. userId={}, analysisRunId={}, durationMs={}",
                    userId, analysisRunId, Duration.between(runStart, Instant.now()).toMillis(), e);
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            analysisRunRepository.markFailed(analysisRunId, truncate(message, ERROR_MESSAGE_MAX_LENGTH));
        }
    }

    private String truncate(String message, int maxLength) {
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }

    // 저장된 결과를 다시 조회해(표시 이름까지 포함된 AnalysisResultDetail) 이벤트로 발행한다.
    // principle이 이 이벤트를 구독해(TendencyAnalyzedEventListener) 추천 후보를 갱신한다.
    private void publishAnalyzedEvent(Long userId, Long analysisRunId) {
        List<AnalysisResultDetail> details = analysisResultRepository.findDetailByAnalysisRunId(analysisRunId);
        List<TendencyAnalyzedEvent.AnalysisResult> results = details.stream()
                .map(d -> new TendencyAnalyzedEvent.AnalysisResult(d.analysisResultId(), d.dimensionCode(), d.typeCode(), d.typeName()))
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new TendencyAnalyzedEvent(userId, analysisRunId, results));
    }

    public List<AnalysisRunSummaryResult> getHistory(GetAnalysisRunsQuery query) {
        return analysisRunRepository.findByUserId(query.userId()).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public AnalysisRunDetailResult getDetail(GetAnalysisRunDetailQuery query) {
        AnalysisRun run = analysisRunRepository.findByIdAndUserId(query.analysisRunId(), query.userId())
                .orElseThrow(() -> new TendencyException(TendencyErrorCode.ANALYSIS_RUN_NOT_FOUND));

        List<AnalysisItemResult> items = analysisResultRepository.findDetailByAnalysisRunId(run.getAnalysisRunId()).stream()
                .map(this::toItemResult)
                .collect(Collectors.toList());

        return new AnalysisRunDetailResult(toSummary(run), items, run.getErrorMessage());
    }

    // auth.domain.ports.TendencyCleanupPort 구현체(TendencyCleanupPortImpl)에서만 호출된다 — 계정
    // 탈퇴 시 사용자의 성향분석 기록을 전부 지운다. analysis_results를 지우기 전에 그 id 목록을
    // principle에 먼저 알려 principle_recommendations도 함께 정리되게 한다 — 순서를 바꾸면(분석
    // 결과를 먼저 지우면) 그 id 목록 자체를 잃어버려 principle 쪽 정리가 불가능해진다.
    @Transactional
    public void deleteAllAnalyses(Long userId) {
        List<Long> analysisResultIds = analysisResultRepository.findIdsByUserId(userId);
        principleRecommendationCleanupPort.deleteRecommendationsForAnalysisResults(analysisResultIds);
        analysisResultRepository.deleteByUserId(userId);
        analysisRunRepository.deleteByUserId(userId);
    }

    // --- 항목별 계산 ---

    private void collectPortfolioRisk(Long userId, List<AnalysisResult> results) {
        try {
            PortfolioRiskAnalysisResult result = portfolioRiskAnalysisService.analyze(new AnalyzePortfolioRiskQuery(userId));
            addResult(results, DIM_PORTFOLIO_RISK, "RISK_" + result.type().name(), result);
        } catch (TendencyException e) {
            log.info("포트폴리오 위험배분 성향 산출 불가 — 이번 실행에서 제외. userId={}, reason={}", userId, e.getMessage());
        }
    }

    private void collectBuyDecisionBasis(Long userId, List<AnalysisResult> results) {
        try {
            RationaleTendencyResult result = rationaleTendencyService.analyze(new AnalyzeRationaleTendencyQuery(userId));
            addResult(results, DIM_BUY_DECISION_BASIS, "BUY_" + result.result().name(), result);
        } catch (TendencyException e) {
            log.info("매수 판단 근거 성향 산출 불가 — 이번 실행에서 제외. userId={}, reason={}", userId, e.getMessage());
        }
    }

    private void collectHoldingPeriod(Long userId, List<AnalysisResult> results) {
        try {
            HoldingPeriodAnalysisResult result = holdingPeriodAnalysisService.analyze(new AnalyzeHoldingPeriodQuery(userId));
            addResult(results, DIM_HOLDING_PERIOD, "PERIOD_" + result.type().name(), result);
        } catch (TendencyException e) {
            log.info("투자 기간 성향 산출 불가 — 이번 실행에서 제외. userId={}, reason={}", userId, e.getMessage());
        }
    }

    private void collectPrincipleAdherence(Long userId, List<AnalysisResult> results) {
        // 원칙이 없거나 검증 가능한 기회가 없어도 INDETERMINATE(판정불가형)로 정상 반환되므로 예외를 잡을 필요가 없다.
        PrincipleAdherenceAnalysisResult result = principleAdherenceAnalysisService.analyze(new AnalyzePrincipleAdherenceQuery(userId));
        addResult(results, DIM_PRINCIPLE_ADHERENCE, "PRINCIPLE_" + result.type().name(), result);
    }

    // 3·4번 공용 — isLoss로 손실/수익 어느 쪽을 집계할지만 가른다.
    private void collectLossOrGain(List<TradeInfo> allTrades, LocalDate windowStart, LocalDate today,
                                    boolean isLoss, List<AnalysisResult> results) {
        if (allTrades.isEmpty()) {
            log.info("{} 성향 산출 불가 — 거래 이력 없음", isLoss ? "손실 대응" : "수익 대응");
            return;
        }

        Map<Long, List<TradeInfo>> tradesBySecurity = allTrades.stream()
                .collect(Collectors.groupingBy(TradeInfo::securityId));

        // 1단계: 종목별로 (손실/수익) 상태였던 날을 세어, 그 종목 자신의 라벨을 먼저 매긴다.
        List<SecurityDayCounts> perSecurityCounts = new ArrayList<>();
        for (Map.Entry<Long, List<TradeInfo>> entry : tradesBySecurity.entrySet()) {
            Map<LocalDate, BigDecimal> closePriceByDay = marketDataPort.findDailyPrices(entry.getKey(), windowStart, today).stream()
                    .collect(Collectors.toMap(DailyPriceInfo::priceDate, DailyPriceInfo::closePrice));
            List<DailyPnlWalker.DailyOutcome> outcomes = DailyPnlWalker.walk(entry.getValue(), closePriceByDay, windowStart, today);

            int totalDays = 0;
            int netSellDays = 0;
            int netBuyDays = 0;
            int holdDays = 0;
            for (DailyPnlWalker.DailyOutcome outcome : outcomes) {
                boolean matches = isLoss ? outcome.pnl().signum() < 0 : outcome.pnl().signum() > 0;
                if (!matches) {
                    continue;
                }
                totalDays++;
                int sign = outcome.netTradeSign();
                if (sign < 0) {
                    netSellDays++;
                } else if (sign > 0) {
                    netBuyDays++;
                } else {
                    holdDays++;
                }
            }
            if (totalDays > 0) {
                perSecurityCounts.add(new SecurityDayCounts(totalDays, netSellDays, netBuyDays, holdDays));
            }
        }

        if (perSecurityCounts.isEmpty()) {
            log.info("{} 성향 산출 불가 — 분석창 내 해당 상태(손실/수익)였던 종목 없음", isLoss ? "손실 대응" : "수익 대응");
            return;
        }

        // 2단계: 종목별 라벨을 다시 종목 단위로 다수결을 내 하나의 최종 라벨로 합산한다.
        if (isLoss) {
            addLossOrGainResult(perSecurityCounts, results, DIM_LOSS_RESPONSE, "LOSS_",
                    LossResponseType.STOP_LOSS, LossResponseType.AVERAGING_DOWN, LossResponseType.HOLD, LossResponseType.MIXED);
        } else {
            addLossOrGainResult(perSecurityCounts, results, DIM_PROFIT_RESPONSE, "PROFIT_",
                    GainResponseType.TAKE_PROFIT, GainResponseType.AVERAGING_UP, GainResponseType.HOLD, GainResponseType.MIXED);
        }
    }

    // sellLabel/buyLabel/holdLabel/mixedLabel 순서로 3번(손실 대응)·4번(수익 대응)의 라벨 타입만 바꿔가며 재사용한다.
    private <T extends Enum<T>> void addLossOrGainResult(List<SecurityDayCounts> perSecurityCounts, List<AnalysisResult> results,
                                                           String dimensionCode, String typePrefix,
                                                           T sellLabel, T buyLabel, T holdLabel, T mixedLabel) {
        List<T> perSecurityLabels = perSecurityCounts.stream()
                .map(c -> ThresholdMajorityLabeler.classify(List.of(
                                new ThresholdMajorityLabeler.Bucket<>(sellLabel, c.netSellDays()),
                                new ThresholdMajorityLabeler.Bucket<>(buyLabel, c.netBuyDays()),
                                new ThresholdMajorityLabeler.Bucket<>(holdLabel, c.holdDays())),
                        c.totalDays(), LOSS_GAIN_THRESHOLD, mixedLabel))
                .collect(Collectors.toList());

        List<ThresholdMajorityLabeler.Bucket<T>> voteBuckets = List.of(
                new ThresholdMajorityLabeler.Bucket<>(sellLabel, Collections.frequency(perSecurityLabels, sellLabel)),
                new ThresholdMajorityLabeler.Bucket<>(buyLabel, Collections.frequency(perSecurityLabels, buyLabel)),
                new ThresholdMajorityLabeler.Bucket<>(holdLabel, Collections.frequency(perSecurityLabels, holdLabel)),
                new ThresholdMajorityLabeler.Bucket<>(mixedLabel, Collections.frequency(perSecurityLabels, mixedLabel))
        );
        T type = ThresholdMajorityLabeler.classify(voteBuckets, perSecurityLabels.size(), LOSS_GAIN_THRESHOLD, mixedLabel);

        addResult(results, dimensionCode, typePrefix + type.name(),
                new SecurityVoteEvidence(perSecurityCounts.size(),
                        Collections.frequency(perSecurityLabels, sellLabel),
                        Collections.frequency(perSecurityLabels, buyLabel),
                        Collections.frequency(perSecurityLabels, holdLabel),
                        Collections.frequency(perSecurityLabels, mixedLabel),
                        type.name()));
    }

    // --- 변환 헬퍼 ---

    private void addResult(List<AnalysisResult> results, String dimensionCode, String typeCode, Object evidence) {
        results.add(AnalysisResult.create(null, dimensionCode, typeCode, toJson(evidence)));
    }

    private String toJson(Object evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (Exception e) {
            log.warn("evidence_json 직렬화 실패 — 빈 객체로 대체", e);
            return "{}";
        }
    }

    private AnalysisRunSummaryResult toSummary(AnalysisRun run) {
        return new AnalysisRunSummaryResult(run.getAnalysisRunId(), run.getPeriodStart(), run.getPeriodEnd(),
                run.getTradeCount(), run.getJournalCount(), run.getAnalysisVersion(), run.getRunStatus(), run.getCreatedAt());
    }

    private AnalysisItemResult toItemResult(AnalysisResultDetail detail) {
        return new AnalysisItemResult(detail.dimensionCode(), detail.dimensionName(),
                detail.typeCode(), detail.typeName(), detail.typeDescription(), detail.evidenceJson());
    }

    // 종목별 1단계 판정에 쓰는 (손실/수익) 상태 일수 집계 — 종목 하나의 결과다.
    private record SecurityDayCounts(int totalDays, int netSellDays, int netBuyDays, int holdDays) {
    }

    // 3·4번 공용 evidence — securityCount는 이 창(window)에서 해당 상태(손실/수익)였던 날이
    // 하루라도 있었던 종목 수. 나머지 카운트는 그 종목들 각각의 1단계 라벨을 다시 종목 단위로 센 것.
    private record SecurityVoteEvidence(int securityCount, int netSellSecurityCount, int netBuySecurityCount,
                                         int holdSecurityCount, int mixedSecurityCount, String type) {
    }
}
