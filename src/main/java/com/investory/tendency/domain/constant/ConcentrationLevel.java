package com.investory.tendency.domain.constant;

// 1번(포트폴리오 위험배분) 분산 축
public enum ConcentrationLevel {
    DIVERSIFIED,   // 분산 — 최대 종목 비중 < CONCENTRATION_THRESHOLD(40%)
    CONCENTRATED   // 집중 — 최대 종목 비중 >= CONCENTRATION_THRESHOLD(40%)
}
