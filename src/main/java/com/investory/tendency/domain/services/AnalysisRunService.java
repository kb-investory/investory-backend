package com.investory.tendency.domain.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.tendency.domain.constant.GainResponseType;
import com.investory.tendency.domain.constant.LossResponseType;
import com.investory.tendency.domain.exception.TendencyErrorCode;
import com.investory.tendency.domain.exception.TendencyException;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;
import com.investory.tendency.domain.model.AnalysisRun;
import com.investory.tendency.domain.ports.MarketDataPort;
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
import com.investory.tendency.domain.services.dto.result.AnalysisRunDetailResult;
import com.investory.tendency.domain.services.dto.result.AnalysisRunSummaryResult;
import com.investory.tendency.domain.services.dto.result.HoldingPeriodAnalysisResult;
import com.investory.tendency.domain.services.dto.result.PortfolioRiskAnalysisResult;
import com.investory.tendency.domain.services.dto.result.PrincipleAdherenceAnalysisResult;
import com.investory.tendency.domain.services.dto.result.RationaleTendencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 6개 항목을 한 번에 실행해서 analysis_runs/analysis_results에 저장하고, 이력/상세를 조회하는 오케스트레이션 서비스.
// 1·2·5·6번은 이미 포트폴리오 전체 기준으로 계산하는 기존 서비스를 그대로 재사용한다.
// 3·4번(손실/수익 대응)은 기존 서비스가 종목 단위(securityId)로만 계산하는데, analysis_results는
// 실행당 항목별 결과가 1행이어야 해서(UNIQUE(analysis_run_id, analysis_dimension_code)) 여기서
// 사용자가 거래한 모든 종목의 일수를 합산해 하나의 라벨로 판정한다 (6번이 이미 종목을 합산하는 것과 동일한 방식).
@Service
public class AnalysisRunService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRunService.class);

    private static final int ANALYSIS_WINDOW_DAYS = 90;
    private static final String ANALYSIS_VERSION = "1.0";
    private static final BigDecimal LOSS_GAIN_THRESHOLD = BigDecimal.valueOf(60, 2);

    private static final String DIM_PORTFOLIO_RISK = "PORTFOLIO_RISK_ALLOCATION";
    private static final String DIM_BUY_DECISION_BASIS = "BUY_DECISION_BASIS";
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
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisRunService(AnalysisRunRepository analysisRunRepository,
                               AnalysisResultRepository analysisResultRepository,
                               PortfolioRiskAnalysisService portfolioRiskAnalysisService,
                               RationaleTendencyService rationaleTendencyService,
                               HoldingPeriodAnalysisService holdingPeriodAnalysisService,
                               PrincipleAdherenceAnalysisService principleAdherenceAnalysisService,
                               TradeLedgerPort tradeLedgerPort,
                               MarketDataPort marketDataPort,
                               ApplicationEventPublisher eventPublisher) {
        this.analysisRunRepository = analysisRunRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.portfolioRiskAnalysisService = portfolioRiskAnalysisService;
        this.rationaleTendencyService = rationaleTendencyService;
        this.holdingPeriodAnalysisService = holdingPeriodAnalysisService;
        this.principleAdherenceAnalysisService = principleAdherenceAnalysisService;
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
        this.eventPublisher = eventPublisher;
    }

    public AnalysisRunDetailResult runAnalysis(RunAnalysisCommand command) {
        Instant runStart = Instant.now();
        Long userId = command.userId();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(ANALYSIS_WINDOW_DAYS - 1L);

        List<TradeInfo> allTrades = tradeLedgerPort.findAllTrades(userId);

        List<AnalysisResult> results = new ArrayList<>();
        collectPortfolioRisk(userId, results);
        collectBuyDecisionBasis(userId, results);
        collectLossOrGain(allTrades, windowStart, today, true, results);
        collectLossOrGain(allTrades, windowStart, today, false, results);
        collectHoldingPeriod(userId, results);
        collectPrincipleAdherence(userId, results);

        // journal_count: journal 도메인이 기간별 일지 건수를 노출하는 Port가 아직 없어 0으로 둔다.
        // TODO: journal에 카운트 조회 기능이 생기면 여기서 채우기.
        AnalysisRun run = AnalysisRun.create(userId, windowStart, today, allTrades.size(), 0, ANALYSIS_VERSION);
        AnalysisRun saved = analysisRunRepository.save(run);

        List<AnalysisResult> resultsWithRunId = results.stream()
                .map(r -> AnalysisResult.create(saved.getAnalysisRunId(), r.getAnalysisDimensionCode(), r.getPrimaryAnalysisTypeCode(), r.getEvidenceJson()))
                .collect(Collectors.toList());
        analysisResultRepository.saveAll(resultsWithRunId);

        publishAnalyzedEvent(userId, saved.getAnalysisRunId());

        AnalysisRunDetailResult detail = getDetail(new GetAnalysisRunDetailQuery(userId, saved.getAnalysisRunId()));

        // 프론트가 실제로 기다리는 POST /tendency/analyses 응답 시간 전체 — 6개 항목 계산을 다 합친 총 소요시간.
        log.info("성향분석 실행 완료. userId={}, analysisRunId={}, tradeCount={}, totalDurationMs={}",
                userId, saved.getAnalysisRunId(), allTrades.size(), Duration.between(runStart, Instant.now()).toMillis());

        return detail;
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

        return new AnalysisRunDetailResult(toSummary(run), items);
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

        int totalDays = 0;
        int netSellDays = 0;
        int netBuyDays = 0;
        int holdDays = 0;

        for (Map.Entry<Long, List<TradeInfo>> entry : tradesBySecurity.entrySet()) {
            Map<LocalDate, BigDecimal> closePriceByDay = marketDataPort.findDailyPrices(entry.getKey(), windowStart, today).stream()
                    .collect(Collectors.toMap(DailyPriceInfo::priceDate, DailyPriceInfo::closePrice));
            List<DailyPnlWalker.DailyOutcome> outcomes = DailyPnlWalker.walk(entry.getValue(), closePriceByDay, windowStart, today);

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
        }

        if (isLoss) {
            List<ThresholdMajorityLabeler.Bucket<LossResponseType>> buckets = List.of(
                    new ThresholdMajorityLabeler.Bucket<>(LossResponseType.STOP_LOSS, netSellDays),
                    new ThresholdMajorityLabeler.Bucket<>(LossResponseType.AVERAGING_DOWN, netBuyDays),
                    new ThresholdMajorityLabeler.Bucket<>(LossResponseType.HOLD, holdDays)
            );
            LossResponseType type = ThresholdMajorityLabeler.classify(buckets, totalDays, LOSS_GAIN_THRESHOLD, LossResponseType.MIXED);
            addResult(results, DIM_LOSS_RESPONSE, "LOSS_" + type.name(),
                    new PortfolioDayCountEvidence(totalDays, netSellDays, netBuyDays, holdDays, type.name()));
        } else {
            List<ThresholdMajorityLabeler.Bucket<GainResponseType>> buckets = List.of(
                    new ThresholdMajorityLabeler.Bucket<>(GainResponseType.TAKE_PROFIT, netSellDays),
                    new ThresholdMajorityLabeler.Bucket<>(GainResponseType.AVERAGING_UP, netBuyDays),
                    new ThresholdMajorityLabeler.Bucket<>(GainResponseType.HOLD, holdDays)
            );
            GainResponseType type = ThresholdMajorityLabeler.classify(buckets, totalDays, LOSS_GAIN_THRESHOLD, GainResponseType.MIXED);
            addResult(results, DIM_PROFIT_RESPONSE, "PROFIT_" + type.name(),
                    new PortfolioDayCountEvidence(totalDays, netSellDays, netBuyDays, holdDays, type.name()));
        }
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
                run.getTradeCount(), run.getJournalCount(), run.getAnalysisVersion(), run.getCreatedAt());
    }

    private AnalysisItemResult toItemResult(AnalysisResultDetail detail) {
        return new AnalysisItemResult(detail.dimensionCode(), detail.dimensionName(),
                detail.typeCode(), detail.typeName(), detail.typeDescription(), detail.evidenceJson());
    }

    // 3·4번(포트폴리오 합산) 전용 evidence — 종목별이 아니라 전체 합산 일수만 담는다.
    private record PortfolioDayCountEvidence(int totalDays, int netSellDays, int netBuyDays, int holdDays, String type) {
    }
}
