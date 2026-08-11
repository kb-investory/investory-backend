package com.investory.tendency.domain.constant;

// 1번(포트폴리오 위험배분) 변동성 축
public enum VolatilityLevel {
    LOW,   // 저변동 — 가중평균 변동성 < VOLATILITY_THRESHOLD
    HIGH   // 고변동 — 가중평균 변동성 >= VOLATILITY_THRESHOLD
}
