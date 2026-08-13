package com.investory.tendency.domain.constant;

// 3번(손실 대응 성향) 최종 라벨
public enum LossResponseType {
    STOP_LOSS,       // 손절형 — 손실 상태에서 최빈 행동이 순매도
    AVERAGING_DOWN,  // 추가매수형 — 손실 상태에서 최빈 행동이 순매수
    HOLD,            // 보유형 — 손실 상태에서 최빈 행동이 보유(무거래)
    MIXED            // 혼합대응형 — 최빈 행동 비율이 판정 기준선(θ) 미만
}
