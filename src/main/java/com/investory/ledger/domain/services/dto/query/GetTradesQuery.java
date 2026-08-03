package com.investory.ledger.domain.services.dto.query;

import java.time.LocalDate;

// tradeSide는 원본 문자열 그대로 받아서 서비스가 직접 파싱·검증한다 (잘못된 값이면 LEDGER_INVALID_TRADE_SIDE).
public record GetTradesQuery(
    Long userId,
    Long accountId,
    Long securityId,
    String tradeSide,
    LocalDate from,
    LocalDate to,
    int page,
    int size
) {
}
