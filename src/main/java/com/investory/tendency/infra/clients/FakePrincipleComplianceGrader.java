package com.investory.tendency.infra.clients;

import com.investory.tendency.domain.constant.PrincipleComplianceGrade;
import com.investory.tendency.domain.ports.PrincipleComplianceGradingPort;
import com.investory.tendency.domain.ports.dto.PrincipleTradingSummary;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// 개발/테스트용 — 실제 LLM 호출 없이 거래 빈도 기반의 단순 휴리스틱으로 대충 흉내만 낸다.
// llm.enabled=false일 때만 활성화된다.
@Component
@Conditional(PrincipleAdherenceLlmDisabledCondition.class)
public class FakePrincipleComplianceGrader implements PrincipleComplianceGradingPort {

    private static final BigDecimal HIGH_FREQUENCY_PER_WEEK = BigDecimal.valueOf(10);
    private static final BigDecimal LOW_FREQUENCY_PER_WEEK = BigDecimal.valueOf(3);

    @Override
    public PrincipleComplianceGrade grade(String principleText, PrincipleTradingSummary summary) {
        // 원칙 문구와 무관하게, 매매가 아주 잦으면 "지나친 매매를 삼간다"류 원칙은 어김에 가깝다고 보고,
        // 아주 뜸하면 지킴에 가깝다고 본다 — 실제 의미 판단은 못 하는 단순 근사치.
        if (summary.avgTradesPerWeek().compareTo(HIGH_FREQUENCY_PER_WEEK) >= 0) {
            return PrincipleComplianceGrade.VIOLATED;
        }
        if (summary.avgTradesPerWeek().compareTo(LOW_FREQUENCY_PER_WEEK) <= 0) {
            return PrincipleComplianceGrade.FOLLOWED;
        }
        return PrincipleComplianceGrade.PARTIAL;
    }
}
