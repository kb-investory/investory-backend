package com.investory.tendency.domain.ports.dto;

import java.math.BigDecimal;

// 추상형(ABSTRACT) 원칙 채점용 포트폴리오 전체 요약 통계 — 원시 거래 데이터는 LLM에 절대 보내지 않는다.
public record PrincipleTradingSummary(
    int totalTradeCountInWindow,
    int distinctSecuritiesTradedInWindow,
    BigDecimal avgTradesPerWeek
) {
}
