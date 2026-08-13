package com.investory.tendency.domain.ports.dto;

import java.math.BigDecimal;

public record HoldingWeightInfo(
    Long securityId,
    BigDecimal portfolioWeight   // 전체 포트폴리오 시가총액 대비 비중(%, 0~100). ledger.HoldingResult.portfolioWeight 그대로.
) {
}
