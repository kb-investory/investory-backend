package com.investory.tendency.infra.clients;

import com.investory.tendency.domain.constant.PrincipleRuleType;
import com.investory.tendency.domain.ports.PrincipleRuleClassificationPort;
import com.investory.tendency.domain.ports.dto.PrincipleRuleClassification;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 개발/테스트용 — 실제 LLM 호출 없이 키워드/숫자 패턴 매칭으로 대충 흉내만 낸다.
// llm.principle-adherence.enabled=false일 때만 활성화된다.
@Component
@Conditional(PrincipleAdherenceLlmDisabledCondition.class)
public class FakePrincipleRuleClassifier implements PrincipleRuleClassificationPort {

    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*%");
    private static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.TEN;

    @Override
    public PrincipleRuleClassification classify(String principleText) {
        if (principleText == null || principleText.isBlank()) {
            return new PrincipleRuleClassification(PrincipleRuleType.ABSTRACT, null);
        }
        String text = principleText.toLowerCase();

        if (containsAny(text, "오르면 매수", "상승하면 매수", "떨어지면 매수", "하락하면 매수")) {
            return new PrincipleRuleClassification(PrincipleRuleType.EXCLUDED, null);
        }
        if (containsAny(text, "손절")) {
            return new PrincipleRuleClassification(PrincipleRuleType.STOP_LOSS, extractPercent(text));
        }
        if (containsAny(text, "익절", "수익 실현", "차익")) {
            return new PrincipleRuleClassification(PrincipleRuleType.TAKE_PROFIT, extractPercent(text));
        }
        return new PrincipleRuleClassification(PrincipleRuleType.ABSTRACT, null);
    }

    private BigDecimal extractPercent(String text) {
        Matcher matcher = PERCENT_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return DEFAULT_THRESHOLD;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
