package com.investory.tendency.domain.services;

import com.investory.tendency.domain.constant.PrincipleAdherenceType;
import com.investory.tendency.domain.constant.PrincipleComplianceGrade;
import com.investory.tendency.domain.constant.PrincipleExclusionReason;
import com.investory.tendency.domain.constant.PrincipleRuleType;
import com.investory.tendency.domain.ports.FakeMarketDataPort;
import com.investory.tendency.domain.ports.FakePrinciplePort;
import com.investory.tendency.domain.ports.FakeTradeLedgerPort;
import com.investory.tendency.domain.ports.PrincipleComplianceGradingPort;
import com.investory.tendency.domain.ports.PrincipleRuleClassificationPort;
import com.investory.tendency.domain.ports.dto.PrincipleRuleClassification;
import com.investory.tendency.domain.ports.dto.PrincipleRuleInfo;
import com.investory.tendency.domain.ports.dto.PrincipleTradingSummary;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import com.investory.tendency.domain.services.dto.query.AnalyzePrincipleAdherenceQuery;
import com.investory.tendency.domain.services.dto.result.AbstractItemResult;
import com.investory.tendency.domain.services.dto.result.NumericItemResult;
import com.investory.tendency.domain.services.dto.result.PrincipleAdherenceAnalysisResult;
import com.investory.tendency.infra.clients.FakePrincipleComplianceGrader;
import com.investory.tendency.infra.clients.FakePrincipleRuleClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 실제 OpenAI 호출 없이, llm.enabled=false일 때 뜨는 것과 동일한
// Fake*Classifier/Grader(키워드/빈도 휴리스틱)를 그대로 써서 6번 로직을 눈으로 확인해보는 테스트.
class PrincipleAdherenceAnalysisServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SECURITY_ID = 100L;

    private FakePrinciplePort principlePort;
    private FakeTradeLedgerPort tradeLedgerPort;
    private FakeMarketDataPort marketDataPort;
    private PrincipleAdherenceAnalysisService service;

    @BeforeEach
    void setUp() {
        principlePort = new FakePrinciplePort();
        tradeLedgerPort = new FakeTradeLedgerPort();
        marketDataPort = new FakeMarketDataPort();
        Executor directExecutor = Runnable::run; // 테스트에서는 호출 스레드에서 그대로 동기 실행
        service = new PrincipleAdherenceAnalysisService(
                principlePort, tradeLedgerPort, marketDataPort,
                new FakePrincipleRuleClassifier(),      // 실제 llm.enabled=false일 때 뜨는 그 빈
                new FakePrincipleComplianceGrader(),    // 위와 동일
                directExecutor);
    }

    @Test
    void 손절_원칙을_어기고_보유하다가_뒤늦게_손절하면_수치형_이행률이_반영된다() {
        // 원칙 텍스트만 주고 ruleJson은 비워서, FakePrincipleRuleClassifier가 "손절"+"10%"를 보고
        // STOP_LOSS/threshold=10으로 직접 분류하게 한다(LLM 호출 스킵 로직이 아니라 분류 자체를 테스트).
        principlePort.add(new PrincipleRuleInfo(1L, "손실이 10% 넘으면 무조건 손절한다", null));
        // 매매가 잦지 않다는 걸 보여주는 정성 원칙 — FakePrincipleComplianceGrader가 저빈도로 보고 FOLLOWED 판정.
        principlePort.add(new PrincipleRuleInfo(2L, "장기적 관점을 유지하며 잦은 매매를 하지 않는다", null));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant buyDay = today.minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant sellDay = today.minusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant();

        tradeLedgerPort.add(new TradeInfo(SECURITY_ID, "BUY", BigDecimal.TEN, BigDecimal.valueOf(10000), buyDay));
        tradeLedgerPort.add(new TradeInfo(SECURITY_ID, "SELL", BigDecimal.TEN, BigDecimal.valueOf(8400), sellDay));

        // day-10(매수일, 판정 제외) / day-5(보유 중, -15% → 손절 기회인데 안 팖 = 미이행)
        marketDataPort.addPrice(SECURITY_ID, today.minusDays(10), 10000);
        marketDataPort.addPrice(SECURITY_ID, today.minusDays(5), 8500);
        // day-3(매도일)은 체결가(8400)로 판정되므로 시세 데이터 불필요

        PrincipleAdherenceAnalysisResult result = service.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));

        // 수치형: day-5(보유, 미이행) + day-3(매도, 이행) = 기회 2, 이행 1
        assertEquals(1, result.numericItems().size());
        NumericItemResult numeric = result.numericItems().get(0);
        assertEquals(PrincipleRuleType.STOP_LOSS, numeric.type());
        assertEquals(2, numeric.opportunities());
        assertEquals(1, numeric.compliance());

        // 추상형: 90일 창에 거래 2건뿐이라 주당 평균이 낮음 → FOLLOWED
        assertEquals(1, result.abstractItems().size());
        AbstractItemResult abstractItem = result.abstractItems().get(0);
        assertEquals(PrincipleComplianceGrade.FOLLOWED, abstractItem.grade());

        // 종합: 기회 2(수치)+1(추상)=3, 이행 1(수치)+1.0(추상 FOLLOWED)=2 → 66.67% → 선택적준수형
        assertEquals(PrincipleAdherenceType.SELECTIVE_COMPLIANCE, result.type());
        BigDecimal expectedRate = BigDecimal.valueOf(200).divide(BigDecimal.valueOf(3), MathContext.DECIMAL64); // 2/3*100
        assertTrue(result.complianceRate().subtract(expectedRate).abs().compareTo(BigDecimal.ONE) < 0);
    }

    // #183 회귀 테스트 — rule_json 컬럼에 SQL NULL이 아니라 JSON literal "null" 텍스트가 들어있어도
    // (시드 데이터 등으로 그렇게 들어갈 수 있다) NPE 없이 SQL NULL과 동일하게 LLM 분류로 폴백해야 한다.
    // objectMapper.readValue("null", ...)가 예외 없이 null을 반환해 raw.getValue()에서 NPE가 났었다.
    @Test
    void ruleJson이_JSON_literal_null_텍스트여도_NPE_없이_LLM_분류로_폴백한다() {
        principlePort.add(new PrincipleRuleInfo(1L, "손실이 10% 넘으면 무조건 손절한다", "null"));

        PrincipleAdherenceAnalysisResult result = service.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));

        assertEquals(1, result.numericItems().size());
        assertEquals(PrincipleRuleType.STOP_LOSS, result.numericItems().get(0).type());
    }

    @Test
    void 원칙이_없으면_판정불가형이다() {
        PrincipleAdherenceAnalysisResult result = service.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));

        assertEquals(PrincipleAdherenceType.INDETERMINATE, result.type());
        assertNull(result.complianceRate());
    }

    @Test
    void 매수_트리거형_원칙은_검증제외로_분류된다() {
        principlePort.add(new PrincipleRuleInfo(1L, "주가가 10% 오르면 매수한다", null));

        PrincipleAdherenceAnalysisResult result = service.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));

        assertEquals(1, result.excludedItems().size());
        assertEquals(PrincipleAdherenceType.INDETERMINATE, result.type());
    }

    // 실제 스레드풀로 병렬 실행이 "정말로 동시에" 일어나는지(순차라면 실패할 정도의 시간 단축)와,
    // 여러 스레드가 각자 결과를 만들어도 유실·중복 없이 항목 수만큼 정확히 모이는지(경합 없음)를 함께 검증한다.
    @Test
    void 원칙_항목별_LLM_호출은_실제로_병렬_실행되고_결과가_유실되지_않는다() {
        int itemCount = 6;
        long perCallDelayMs = 200;
        Set<String> seenThreadNames = new CopyOnWriteArraySet<>();

        Executor realExecutor = Executors.newFixedThreadPool(itemCount);
        PrincipleRuleClassificationPort slowClassifier = principleText -> {
            seenThreadNames.add(Thread.currentThread().getName());
            sleep(perCallDelayMs);
            return new PrincipleRuleClassification(PrincipleRuleType.ABSTRACT, null);
        };
        PrincipleComplianceGradingPort slowGrader = (principleText, summary) -> {
            seenThreadNames.add(Thread.currentThread().getName());
            sleep(perCallDelayMs);
            return PrincipleComplianceGrade.FOLLOWED;
        };
        PrincipleAdherenceAnalysisService parallelService = new PrincipleAdherenceAnalysisService(
                principlePort, tradeLedgerPort, marketDataPort, slowClassifier, slowGrader, realExecutor);

        IntStream.rangeClosed(1, itemCount)
                .forEach(i -> principlePort.add(new PrincipleRuleInfo((long) i, "장기적 관점을 유지한다 " + i, null)));

        Instant start = Instant.now();
        PrincipleAdherenceAnalysisResult result = parallelService.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));
        long elapsedMs = java.time.Duration.between(start, Instant.now()).toMillis();

        // 항목이 전부 ABSTRACT라 분류 단계 + 채점 단계 두 번의 병렬 라운드를 거친다.
        // 순차였다면 2 * itemCount * perCallDelayMs = 2400ms인데, 병렬이면 라운드당 perCallDelayMs 근처로 끝난다.
        assertTrue(elapsedMs < itemCount * perCallDelayMs,
                "병렬 실행이라면 " + (itemCount * perCallDelayMs) + "ms보다 훨씬 짧아야 하는데 " + elapsedMs + "ms 걸림");
        assertTrue(seenThreadNames.size() > 1, "여러 스레드에서 실행됐어야 하는데 " + seenThreadNames + "만 관찰됨");

        // 경합 없이 6개 항목 결과가 정확히 다 모였는지 확인
        assertEquals(itemCount, result.abstractItems().size());
        Set<Long> resultItemIds = result.abstractItems().stream()
                .map(AbstractItemResult::principleItemId)
                .collect(Collectors.toSet());
        assertEquals(itemCount, resultItemIds.size()); // 중복 없음
        assertEquals(
                IntStream.rangeClosed(1, itemCount).mapToObj(i -> (long) i).collect(Collectors.toSet()),
                resultItemIds); // 유실 없음
    }

    // 부하가 몰려 tendencyLlmExecutor 큐가 가득 차면 CompletableFuture.supplyAsync() 제출 자체가
    // RejectedExecutionException을 던진다(1000 VU 부하테스트에서 실제로 재현됨 — k6 에러 151건이
    // 전부 이 경로였다). classifyItem()의 자체 catch는 "제출된 작업이 실행 중 실패"하는 경우만
    // 잡으므로, 제출 시점 거부는 별도로 흡수해야 한다.
    @Test
    void 원칙_분류_작업_제출이_거부되면_CLASSIFICATION_FAILED로_대체된다() {
        principlePort.add(new PrincipleRuleInfo(1L, "손실이 10% 넘으면 무조건 손절한다", null));

        Executor alwaysRejectingExecutor = command -> {
            throw new RejectedExecutionException("simulated queue saturation");
        };
        PrincipleAdherenceAnalysisService rejectingService = new PrincipleAdherenceAnalysisService(
                principlePort, tradeLedgerPort, marketDataPort,
                new FakePrincipleRuleClassifier(), new FakePrincipleComplianceGrader(), alwaysRejectingExecutor);

        PrincipleAdherenceAnalysisResult result = rejectingService.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));

        assertEquals(1, result.excludedItems().size());
        assertEquals(PrincipleExclusionReason.CLASSIFICATION_FAILED, result.excludedItems().get(0).reason());
    }

    // 위와 같은 이유로 채점 단계(evaluateAbstractItems -> submitGrading)의 제출 거부도 흡수해야 한다.
    // 분류 단계는 통과시키고 채점 단계에서만 거부되도록, 첫 제출(분류) 이후부터 거부하는 실행기를 쓴다.
    @Test
    void 원칙_준수_채점_작업_제출이_거부되면_GRADING_FAILED로_대체된다() {
        principlePort.add(new PrincipleRuleInfo(1L, "장기적 관점을 유지하며 잦은 매매를 하지 않는다", null));

        Executor delegate = Executors.newFixedThreadPool(2);
        AtomicInteger submissionCount = new AtomicInteger(0);
        Executor rejectAfterClassification = command -> {
            if (submissionCount.incrementAndGet() > 1) {
                throw new RejectedExecutionException("simulated queue saturation");
            }
            delegate.execute(command);
        };
        PrincipleAdherenceAnalysisService rejectingService = new PrincipleAdherenceAnalysisService(
                principlePort, tradeLedgerPort, marketDataPort,
                new FakePrincipleRuleClassifier(), new FakePrincipleComplianceGrader(), rejectAfterClassification);

        PrincipleAdherenceAnalysisResult result = rejectingService.analyze(new AnalyzePrincipleAdherenceQuery(USER_ID));

        assertEquals(1, result.excludedItems().size());
        assertEquals(PrincipleExclusionReason.GRADING_FAILED, result.excludedItems().get(0).reason());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
