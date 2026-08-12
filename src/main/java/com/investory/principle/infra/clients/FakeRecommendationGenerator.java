package com.investory.principle.infra.clients;

import com.investory.principle.domain.ports.RecommendationGenerationPort;
import com.investory.principle.domain.ports.dto.GeneratedRecommendation;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.List;

// 개발/테스트용 — 실제 LLM 호출 없이 성향 코드에 따라 고정 문구를 대충 흉내만 낸다.
// llm.enabled=false일 때만 활성화된다.
@Component
@Conditional(RecommendationGenerationDisabledCondition.class)
public class FakeRecommendationGenerator implements RecommendationGenerationPort {

    @Override
    public List<GeneratedRecommendation> generate(String analysisTypeCode, String analysisTypeName) {
        return List.of(new GeneratedRecommendation(
                analysisTypeName + " 성향에 맞춰 투자 원칙을 점검해본다.",
                "성향 분석 결과(" + analysisTypeCode + ")를 바탕으로 한 임시 추천입니다.",
                null));
    }
}
