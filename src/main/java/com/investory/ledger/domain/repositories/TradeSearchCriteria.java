package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.constant.TradeSide;

import java.time.LocalDate;
import java.util.List;

// accountIds는 서비스 계층에서 이미 사용자 소유 계좌로 해석해서 넘긴다 — Repository는 소유권을 모른다.
public record TradeSearchCriteria(
    List<Long> accountIds,
    Long securityId,
    TradeSide tradeSide,
    LocalDate from,
    LocalDate to,
    int page,
    int size
) {
}
