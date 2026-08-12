package com.investory.tendency.domain.services;

import com.investory.tendency.domain.constant.PrincipleAdherenceType;
import com.investory.tendency.domain.constant.PrincipleComplianceGrade;
import com.investory.tendency.domain.constant.PrincipleRuleType;
import com.investory.tendency.domain.ports.FakeMarketDataPort;
import com.investory.tendency.domain.ports.FakePrinciplePort;
import com.investory.tendency.domain.ports.FakeTradeLedgerPort;
import com.investory.tendency.domain.ports.dto.PrincipleRuleInfo;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 실제 OpenAI 호출 없이, llm.principle-adherence.enabled=false일 때 뜨는 것과 동일한
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
        service = new PrincipleAdherenceAnalysisService(
                principlePort, tradeLedgerPort, marketDataPort,
                new FakePrincipleRuleClassifier(),      // 실제 llm.enabled=false일 때 뜨는 그 빈
                new FakePrincipleComplianceGrader());   // 위와 동일
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
}
