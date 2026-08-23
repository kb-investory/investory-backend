package com.investory.tendency.domain.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.tendency.domain.constant.PrincipleAdherenceType;
import com.investory.tendency.domain.constant.PrincipleComplianceGrade;
import com.investory.tendency.domain.constant.PrincipleExclusionReason;
import com.investory.tendency.domain.constant.PrincipleRuleType;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.PrincipleComplianceGradingPort;
import com.investory.tendency.domain.ports.PrinciplePort;
import com.investory.tendency.domain.ports.PrincipleRuleClassificationPort;
import com.investory.tendency.domain.ports.TradeLedgerPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import com.investory.tendency.domain.ports.dto.PrincipleRuleClassification;
import com.investory.tendency.domain.ports.dto.PrincipleRuleInfo;
import com.investory.tendency.domain.ports.dto.PrincipleTradingSummary;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import com.investory.tendency.domain.services.dto.query.AnalyzePrincipleAdherenceQuery;
import com.investory.tendency.domain.services.dto.result.AbstractItemResult;
import com.investory.tendency.domain.services.dto.result.ExcludedItemResult;
import com.investory.tendency.domain.services.dto.result.NumericItemResult;
import com.investory.tendency.domain.services.dto.result.PrincipleAdherenceAnalysisResult;
import com.investory.tendency.infra.exception.PrincipleAdherenceLlmException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

// 원칙 이행 성향(6번) 분석. 3·4번과 달리 특정 종목이 아니라 유저의 활성 원칙 세트 전체를 대상으로 한다.
// 수치형(손절/익절) 원칙은 DailyPnlWalker로 일별 손익을 재계산해 규칙 위반 여부를 직접 판정하고,
// 정성적 원칙은 포트폴리오 요약 통계를 LLM에 전달해 등급을 매긴다. 둘을 "기회/이행" 단위로 합산해
// 종합 준수율을 낸다. 원칙이 없거나 검증 가능한 기회가 하나도 없으면 판정불가형(정상 결과, 예외 아님).
@Service
public class PrincipleAdherenceAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PrincipleAdherenceAnalysisService.class);

    private static final int ANALYSIS_WINDOW_DAYS = 90;
    // 최빈 행동 비율이 아니라 준수율 자체에 대한 경계선 — 데이터 보며 튜닝할 값이라 상수로 분리해둔다.
    private static final BigDecimal ALIGNED_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal SELECTIVE_THRESHOLD = BigDecimal.valueOf(40);
    private static final BigDecimal PARTIAL_SCORE = BigDecimal.valueOf(50, 2); // 0.5

    private final PrinciplePort principlePort;
    private final TradeLedgerPort tradeLedgerPort;
    private final MarketDataPort marketDataPort;
    private final PrincipleRuleClassificationPort ruleClassificationPort;
    private final PrincipleComplianceGradingPort complianceGradingPort;
    private final Executor tendencyLlmExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PrincipleAdherenceAnalysisService(PrinciplePort principlePort,
                                              TradeLedgerPort tradeLedgerPort,
                                              MarketDataPort marketDataPort,
                                              PrincipleRuleClassificationPort ruleClassificationPort,
                                              PrincipleComplianceGradingPort complianceGradingPort,
                                              @Qualifier("tendencyLlmExecutor") Executor tendencyLlmExecutor) {
        this.principlePort = principlePort;
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
        this.ruleClassificationPort = ruleClassificationPort;
        this.complianceGradingPort = complianceGradingPort;
        this.tendencyLlmExecutor = tendencyLlmExecutor;
    }

    public PrincipleAdherenceAnalysisResult analyze(AnalyzePrincipleAdherenceQuery query) {
        Instant analyzeStart = Instant.now();
        List<PrincipleRuleInfo> principles = principlePort.findActivePrincipleRules(query.userId());
        if (principles.isEmpty()) {
            return PrincipleAdherenceAnalysisResult.indeterminate();
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(ANALYSIS_WINDOW_DAYS - 1L);

        List<ItemClassification> classified = classifyAll(principles, query.userId());

        List<ItemClassification> numeric = classified.stream()
                .filter(c -> c.type() == PrincipleRuleType.STOP_LOSS || c.type() == PrincipleRuleType.TAKE_PROFIT)
                .collect(Collectors.toList());
        List<ItemClassification> abstractItems = classified.stream()
                .filter(c -> c.type() == PrincipleRuleType.ABSTRACT)
                .collect(Collectors.toList());
        List<ExcludedItemResult> excluded = classified.stream()
                .filter(c -> c.type() == PrincipleRuleType.EXCLUDED)
                .map(this::toExcludedResult)
                .collect(Collectors.toList());

        // numeric·abstract 둘 다 없으면(전부 EXCLUDED) 포트폴리오 조회 자체를 생략
        List<TradeInfo> allTrades = (numeric.isEmpty() && abstractItems.isEmpty())
                ? List.of()
                : tradeLedgerPort.findAllTrades(query.userId());

        List<NumericItemResult> numericResults = evaluateNumericItems(numeric, allTrades, windowStart, today);
        AbstractEvaluation abstractEval = evaluateAbstractItems(abstractItems, allTrades, windowStart, today, query.userId());

        List<ExcludedItemResult> allExcluded = new ArrayList<>(excluded);
        allExcluded.addAll(abstractEval.newlyExcluded());

        PrincipleAdherenceAnalysisResult result = composite(numericResults, abstractEval.results(), allExcluded);

        log.info("원칙 이행 성향 분석 완료. userId={}, principleCount={}, totalDurationMs={}",
                query.userId(), principles.size(), Duration.between(analyzeStart, Instant.now()).toMillis());
        return result;
    }

    // --- 분류 단계 ---

    // 원칙 항목마다 순서대로 LLM을 호출하면 항목 수만큼 지연이 그대로 누적되므로, tendencyLlmExecutor로
    // 병렬 실행하고 전부 끝날 때까지 join()으로 기다린다. classifyItem 자체가 이미
    // PrincipleAdherenceLlmException을 내부에서 잡아 EXCLUDED로 대체하므로, 항목 하나의 실패가
    // join()으로 전파되어 다른 항목까지 막지 않는다. tendencyLlmExecutor 큐가 가득 차 작업 제출 자체가
    // 거부되는 경우(RejectedExecutionException)도 같은 이유로 EXCLUDED로 대체한다 — classify() 호출
    // 실패와 마찬가지로 요청 전체를 실패시킬 이유가 아니다.
    private List<ItemClassification> classifyAll(List<PrincipleRuleInfo> principles, Long userId) {
        Instant phaseStart = Instant.now();
        List<CompletableFuture<ItemClassification>> futures = principles.stream()
                .map(this::submitClassification)
                .collect(Collectors.toList());
        List<ItemClassification> classified = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        log.info("원칙 분류 단계 완료. userId={}, itemCount={}, durationMs={}",
                userId, principles.size(), Duration.between(phaseStart, Instant.now()).toMillis());
        return classified;
    }

    private CompletableFuture<ItemClassification> submitClassification(PrincipleRuleInfo item) {
        try {
            return CompletableFuture.supplyAsync(() -> classifyItem(item), tendencyLlmExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("원칙 분류 작업 제출 실패(큐 포화) — EXCLUDED로 대체합니다. principleItemId={}",
                    item.principleItemId(), e);
            return CompletableFuture.completedFuture(
                    new ItemClassification(item, PrincipleRuleType.EXCLUDED, null, PrincipleExclusionReason.CLASSIFICATION_FAILED));
        }
    }

    private ItemClassification classifyItem(PrincipleRuleInfo item) {
        RuleJsonShape parsed = tryParseKnownRuleType(item.ruleJson());
        if (parsed != null) {
            return new ItemClassification(item, parsed.type(), parsed.value(), null);
        }
        Instant callStart = Instant.now();
        try {
            PrincipleRuleClassification result = ruleClassificationPort.classify(item.principleText());
            log.info("원칙 분류 LLM 호출 완료. principleItemId={}, durationMs={}",
                    item.principleItemId(), Duration.between(callStart, Instant.now()).toMillis());
            return new ItemClassification(item, result.type(), result.thresholdPercent(), null);
        } catch (PrincipleAdherenceLlmException e) {
            log.warn("원칙 규칙 분류 실패 — 이번 실행에서 제외합니다. principleItemId={}, durationMs={}",
                    item.principleItemId(), Duration.between(callStart, Instant.now()).toMillis(), e);
            return new ItemClassification(item, PrincipleRuleType.EXCLUDED, null, PrincipleExclusionReason.CLASSIFICATION_FAILED);
        }
    }

    // ruleJson이 {"type":"STOP_LOSS"|"TAKE_PROFIT","value":<number>,"unit":"PERCENT"} 형태로 이미
    // 파싱되면 그대로 쓰고 LLM 호출을 스킵한다. 그 외 타입(예: MAX_POSITION_RATIO)이거나 파싱 자체가
    // 안 되면 null을 반환해 LLM 분류로 폴백한다.
    private RuleJsonShape tryParseKnownRuleType(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            return null;
        }
        RawRuleJson raw;
        try {
            raw = objectMapper.readValue(ruleJson, RawRuleJson.class);
        } catch (Exception e) {
            return null;
        }
        // ruleJson이 SQL NULL이 아니라 JSON literal "null" 텍스트인 경우 — Jackson이 예외 없이
        // raw=null을 반환하므로 SQL NULL과 동일하게 취급한다(#183).
        if (raw == null || raw.getValue() == null) {
            return null;
        }
        if ("STOP_LOSS".equals(raw.getType())) {
            return new RuleJsonShape(PrincipleRuleType.STOP_LOSS, raw.getValue());
        }
        if ("TAKE_PROFIT".equals(raw.getType())) {
            return new RuleJsonShape(PrincipleRuleType.TAKE_PROFIT, raw.getValue());
        }
        return null;
    }

    private ExcludedItemResult toExcludedResult(ItemClassification item) {
        PrincipleExclusionReason reason = item.failureReason() != null
                ? item.failureReason()
                : PrincipleExclusionReason.UNVERIFIABLE_BY_DESIGN;
        return new ExcludedItemResult(item.source().principleItemId(), item.source().principleText(), reason);
    }

    // --- 수치형 평가 ---

    private List<NumericItemResult> evaluateNumericItems(List<ItemClassification> numeric, List<TradeInfo> allTrades,
                                                           LocalDate windowStart, LocalDate today) {
        if (numeric.isEmpty()) {
            return List.of();
        }

        // 평가 대상 종목 = allTrades(전체 이력, 기간 제한 없음) 기준 distinct 종목 전부 — 90일 창
        // 안에 거래가 없어도 포함해야, 창 이전에 사서 계속 들고만 있다 손절 원칙을 어긴 경우를 놓치지 않는다.
        // allTrades는 이미 유저 전체 이력이라 종목별로 다시 조회하지 않고 로컬에서 묶어 재사용한다.
        Map<Long, List<TradeInfo>> tradesBySecurity = allTrades.stream()
                .collect(Collectors.groupingBy(TradeInfo::securityId));

        // 종목별 day-walk는 원칙 항목 개수와 무관하므로 종목당 1회만 계산해 재사용한다.
        Map<Long, List<DailyPnlWalker.DailyOutcome>> outcomesBySecurity = new HashMap<>();
        for (Map.Entry<Long, List<TradeInfo>> entry : tradesBySecurity.entrySet()) {
            Long securityId = entry.getKey();
            Map<LocalDate, BigDecimal> closePriceByDay = marketDataPort.findDailyPrices(securityId, windowStart, today).stream()
                    .collect(Collectors.toMap(DailyPriceInfo::priceDate, DailyPriceInfo::closePrice));
            outcomesBySecurity.put(securityId, DailyPnlWalker.walk(entry.getValue(), closePriceByDay, windowStart, today));
        }

        List<NumericItemResult> results = new ArrayList<>();
        for (ItemClassification item : numeric) {
            int opportunities = 0;
            int compliance = 0;
            for (List<DailyPnlWalker.DailyOutcome> outcomes : outcomesBySecurity.values()) {
                for (DailyPnlWalker.DailyOutcome outcome : outcomes) {
                    boolean isOpportunity = item.type() == PrincipleRuleType.STOP_LOSS
                            ? outcome.returnRate().compareTo(item.thresholdPercent().negate()) <= 0
                            : outcome.returnRate().compareTo(item.thresholdPercent()) >= 0;
                    if (!isOpportunity) {
                        continue;
                    }
                    opportunities++;
                    if (outcome.netTradeSign() < 0) {
                        compliance++;
                    }
                }
            }
            results.add(new NumericItemResult(item.source().principleItemId(), item.source().principleText(),
                    item.type(), item.thresholdPercent(), opportunities, compliance));
        }
        return results;
    }

    // --- 추상형 평가 ---

    private AbstractEvaluation evaluateAbstractItems(List<ItemClassification> abstractItems, List<TradeInfo> allTrades,
                                                       LocalDate windowStart, LocalDate today, Long userId) {
        if (abstractItems.isEmpty()) {
            return new AbstractEvaluation(List.of(), List.of());
        }

        List<TradeInfo> windowTrades = allTrades.stream()
                .filter(t -> isInWindow(t.tradedAt(), windowStart, today))
                .collect(Collectors.toList());
        int totalTradeCount = windowTrades.size();
        int distinctSecurities = (int) windowTrades.stream().map(TradeInfo::securityId).distinct().count();
        BigDecimal weeks = BigDecimal.valueOf(ANALYSIS_WINDOW_DAYS).divide(BigDecimal.valueOf(7), MathContext.DECIMAL64);
        BigDecimal avgPerWeek = totalTradeCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalTradeCount).divide(weeks, MathContext.DECIMAL64);
        PrincipleTradingSummary summary = new PrincipleTradingSummary(totalTradeCount, distinctSecurities, avgPerWeek);

        // 분류 단계와 같은 이유로 병렬 실행한다 — gradeItem도 PrincipleAdherenceLlmException을 내부에서
        // 잡아 GradeOutcome.excluded()로 대체하므로 항목 하나의 실패가 다른 항목에 번지지 않는다.
        // classifyAll()과 같은 이유로 작업 제출 자체가 거부되는 경우(RejectedExecutionException)도
        // GradeOutcome.excluded()로 흡수한다.
        Instant phaseStart = Instant.now();
        List<CompletableFuture<GradeOutcome>> futures = abstractItems.stream()
                .map(item -> submitGrading(item, summary))
                .collect(Collectors.toList());
        List<GradeOutcome> outcomes = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        log.info("원칙 준수 채점 단계 완료. userId={}, itemCount={}, durationMs={}",
                userId, abstractItems.size(), Duration.between(phaseStart, Instant.now()).toMillis());

        List<AbstractItemResult> results = outcomes.stream()
                .map(GradeOutcome::result)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<ExcludedItemResult> newlyExcluded = outcomes.stream()
                .map(GradeOutcome::excluded)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new AbstractEvaluation(results, newlyExcluded);
    }

    private GradeOutcome gradeItem(ItemClassification item, PrincipleTradingSummary summary) {
        Instant callStart = Instant.now();
        try {
            PrincipleComplianceGrade grade = complianceGradingPort.grade(item.source().principleText(), summary);
            log.info("원칙 준수 채점 LLM 호출 완료. principleItemId={}, durationMs={}",
                    item.source().principleItemId(), Duration.between(callStart, Instant.now()).toMillis());
            return GradeOutcome.success(new AbstractItemResult(item.source().principleItemId(), item.source().principleText(), grade));
        } catch (PrincipleAdherenceLlmException e) {
            log.warn("원칙 준수 채점 실패 — 이번 실행에서 제외합니다. principleItemId={}, durationMs={}",
                    item.source().principleItemId(), Duration.between(callStart, Instant.now()).toMillis(), e);
            return GradeOutcome.excluded(new ExcludedItemResult(item.source().principleItemId(), item.source().principleText(),
                    PrincipleExclusionReason.GRADING_FAILED));
        }
    }

    private CompletableFuture<GradeOutcome> submitGrading(ItemClassification item, PrincipleTradingSummary summary) {
        try {
            return CompletableFuture.supplyAsync(() -> gradeItem(item, summary), tendencyLlmExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("원칙 준수 채점 작업 제출 실패(큐 포화) — GRADING_FAILED로 대체합니다. principleItemId={}",
                    item.source().principleItemId(), e);
            return CompletableFuture.completedFuture(GradeOutcome.excluded(
                    new ExcludedItemResult(item.source().principleItemId(), item.source().principleText(),
                            PrincipleExclusionReason.GRADING_FAILED)));
        }
    }

    private boolean isInWindow(Instant tradedAt, LocalDate windowStart, LocalDate today) {
        LocalDate date = tradedAt.atZone(ZoneOffset.UTC).toLocalDate();
        return !date.isBefore(windowStart) && !date.isAfter(today);
    }

    // --- 종합 ---

    private PrincipleAdherenceAnalysisResult composite(List<NumericItemResult> numericResults,
            List<AbstractItemResult> abstractResults, List<ExcludedItemResult> excludedResults) {
        BigDecimal numericOpportunities = numericResults.stream()
                .map(r -> BigDecimal.valueOf(r.opportunities()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal numericCompliance = numericResults.stream()
                .map(r -> BigDecimal.valueOf(r.compliance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOpportunities = numericOpportunities.add(BigDecimal.valueOf(abstractResults.size()));
        BigDecimal totalCompliance = numericCompliance.add(
                abstractResults.stream().map(this::gradeScore).reduce(BigDecimal.ZERO, BigDecimal::add));

        if (totalOpportunities.signum() == 0) {
            return PrincipleAdherenceAnalysisResult.indeterminate(numericResults, abstractResults, excludedResults);
        }

        BigDecimal rate = totalCompliance.divide(totalOpportunities, MathContext.DECIMAL64).multiply(BigDecimal.valueOf(100));
        PrincipleAdherenceType type = rate.compareTo(ALIGNED_THRESHOLD) >= 0 ? PrincipleAdherenceType.PRINCIPLE_ALIGNED
                : rate.compareTo(SELECTIVE_THRESHOLD) >= 0 ? PrincipleAdherenceType.SELECTIVE_COMPLIANCE
                : PrincipleAdherenceType.REPEATED_DEVIATION;

        return new PrincipleAdherenceAnalysisResult(totalOpportunities, totalCompliance, rate, type,
                numericResults, abstractResults, excludedResults);
    }

    private BigDecimal gradeScore(AbstractItemResult result) {
        return switch (result.grade()) {
            case FOLLOWED -> BigDecimal.ONE;
            case PARTIAL -> PARTIAL_SCORE;
            case VIOLATED -> BigDecimal.ZERO;
        };
    }

    // --- 내부 헬퍼 타입 ---

    private record ItemClassification(PrincipleRuleInfo source, PrincipleRuleType type,
                                       BigDecimal thresholdPercent, PrincipleExclusionReason failureReason) {
    }

    private record RuleJsonShape(PrincipleRuleType type, BigDecimal value) {
    }

    private record AbstractEvaluation(List<AbstractItemResult> results, List<ExcludedItemResult> newlyExcluded) {
    }

    // gradeItem()의 병렬 실행 결과 — 성공/실패 중 정확히 하나만 채워진다.
    private record GradeOutcome(AbstractItemResult result, ExcludedItemResult excluded) {
        static GradeOutcome success(AbstractItemResult result) {
            return new GradeOutcome(result, null);
        }

        static GradeOutcome excluded(ExcludedItemResult excluded) {
            return new GradeOutcome(null, excluded);
        }
    }

    // 기존 ruleJson 저장 형태({"type":"...","value":N,"unit":"..."})를 그대로 읽기 위한 파싱용 DTO.
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawRuleJson {
        private String type;
        private BigDecimal value;
        private String unit;
    }
}
