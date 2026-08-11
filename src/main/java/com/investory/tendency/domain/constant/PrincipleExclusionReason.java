package com.investory.tendency.domain.constant;

// 원칙 항목이 종합 점수 계산에서 제외된 이유 — 응답에서 "설계상 검증 불가"와
// "LLM 호출 실패"를 구분해 보여주기 위함.
public enum PrincipleExclusionReason {
    UNVERIFIABLE_BY_DESIGN,   // 분류 결과 EXCLUDED (매수 트리거 등, 정상적인 판정)
    CLASSIFICATION_FAILED,    // 규칙 분류 LLM 호출 자체가 실패
    GRADING_FAILED            // 준수 채점 LLM 호출 자체가 실패
}
