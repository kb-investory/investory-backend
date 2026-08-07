package com.investory.ledger.domain.services.dto.command;

import java.math.BigDecimal;

// broker가 이미 계산해서 넘기는 값 — ledger는 trades를 재생해서 검산하지 않고 그대로 신뢰한다.
public record RawHoldingRecord(
    String securityCode,
    BigDecimal quantity,
    BigDecimal averagePurchasePrice,
    BigDecimal currentPrice
) {
}
