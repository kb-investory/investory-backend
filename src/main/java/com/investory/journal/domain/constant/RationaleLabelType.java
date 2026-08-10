package com.investory.journal.domain.constant;

public enum RationaleLabelType {
    FUNDAMENTAL_ANALYSIS,      // 기업분석형 — 실적·재무·사업경쟁력
    PRICE_TREND,               // 가격흐름형 — 차트·기술적 흐름·이동평균
    EVENT_REACTION,            // 이벤트반응형 — 뉴스·공시·정책·테마
    INTUITION_SOCIAL_SIGNAL,   // 직관·사회신호형 — 감·지인추천·커뮤니티
    UNCLASSIFIED                // 분류안됨 — LLM 판단 보류 또는 라벨링 실패
}
