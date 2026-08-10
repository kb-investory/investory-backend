package com.investory.tendency.domain.constant;

// 4번(수익 대응 성향) 최종 라벨
public enum GainResponseType {
    TAKE_PROFIT,   // 차익실현형 — 수익 상태에서 최빈 행동이 순매도
    AVERAGING_UP,  // 추가매수형 — 수익 상태에서 최빈 행동이 순매수
    HOLD,          // 보유형 — 수익 상태에서 최빈 행동이 보유(무거래)
    MIXED          // 혼합대응형 — 최빈 행동 비율이 판정 기준선(θ) 미만
}
