package com.investory.journal.infra.clients;

import com.investory.journal.domain.constant.RationaleLabelType;
import com.investory.journal.domain.ports.RationaleLabelingPort;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

// 개발/테스트용 — 실제 LLM 호출 없이 키워드 매칭으로 대충 흉내만 낸다.
// llm.rationale-labeling.enabled=false일 때만 활성화된다.
@Component
@Conditional(RationaleLabelingDisabledCondition.class)
public class FakeRationaleLabelingAdapter implements RationaleLabelingPort {

    @Override
    public RationaleLabelType classify(String rationaleText) {
        if (rationaleText == null || rationaleText.isBlank()) {
            return RationaleLabelType.UNCLASSIFIED;
        }
        String text = rationaleText.toLowerCase();
        if (containsAny(text, "실적", "재무", "매출", "영업이익", "경쟁력")) {
            return RationaleLabelType.FUNDAMENTAL_ANALYSIS;
        }
        if (containsAny(text, "차트", "이동평균", "지지선", "저항선", "추세")) {
            return RationaleLabelType.PRICE_TREND;
        }
        if (containsAny(text, "뉴스", "공시", "정책", "테마", "이슈")) {
            return RationaleLabelType.EVENT_REACTION;
        }
        if (containsAny(text, "감", "지인", "추천", "커뮤니티", "느낌")) {
            return RationaleLabelType.INTUITION_SOCIAL_SIGNAL;
        }
        return RationaleLabelType.UNCLASSIFIED;
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
