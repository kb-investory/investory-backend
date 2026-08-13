package com.investory.tendency.domain.constant;

// 1번(포트폴리오 위험배분) 최종 라벨. ConcentrationLevel × VolatilityLevel 2x2 조합.
public enum PortfolioRiskType {
    LOW_VOLATILITY_DIVERSIFIED,    // 저변동 분산형 — 분산 + 저변동
    HIGH_VOLATILITY_DIVERSIFIED,   // 고변동 분산형 — 분산 + 고변동
    LOW_VOLATILITY_CONCENTRATED,   // 저변동 집중형 — 집중 + 저변동
    HIGH_VOLATILITY_CONCENTRATED   // 고변동 집중형 — 집중 + 고변동
}
