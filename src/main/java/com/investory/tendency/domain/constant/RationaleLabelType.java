package com.investory.tendency.domain.constant;

// journal_trade_notes.rationale_label 컬럼에 저장되는 판단 근거 유형.
// 이미 다른 곳(별도 분류 파이프라인)에서 분류되어 DB에 저장된 값을 그대로 신뢰해서 집계에만 사용한다.
// 이 기능에서는 rationale_text를 다시 분석하거나 라벨을 새로 분류하지 않는다.
public enum RationaleLabelType {
    FUNDAMENTAL_ANALYSIS,       // 기업분석형
    PRICE_TREND,                // 가격흐름형
    EVENT_REACTION,             // 이벤트반응형
    INTUITION_SOCIAL_SIGNAL,    // 직관·사회신호형
    UNCLASSIFIED                // 분류되지 않은 판단 근거
}
