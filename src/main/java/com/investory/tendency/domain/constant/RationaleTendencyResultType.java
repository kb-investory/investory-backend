package com.investory.tendency.domain.constant;

// 최근 90일 판단 근거(rationale_label) 집계 결과로 결정되는 최종 투자 성향.
//
// COMPLEX는 DB에 저장되는 개별 rationale_label 값이 아니다 — RationaleLabelType과는 별개의 enum이다.
// 4개 주요 유형(FUNDAMENTAL_ANALYSIS/PRICE_TREND/EVENT_REACTION/INTUITION_SOCIAL_SIGNAL) 중
// 어느 하나도 Threshold(60%) 이상을 차지하지 못했을 때만 계산 결과로 만들어진다.
public enum RationaleTendencyResultType {
    FUNDAMENTAL_ANALYSIS,
    PRICE_TREND,
    EVENT_REACTION,
    INTUITION_SOCIAL_SIGNAL,
    COMPLEX
}
