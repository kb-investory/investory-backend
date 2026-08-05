package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.AccountHoldingsInfo;

// 계좌의 종목별 보유 상세(holding_snapshots 기반)를 ledger에 위임한다 —
// broker는 holding_snapshots를 소유하지 않으므로 직접 조회하지 않는다.
// HoldingSummaryPort(요약 전용)와 반환 단위가 달라 별도 Port로 분리한다.
public interface HoldingDetailPort {
    AccountHoldingsInfo getHoldings(Long userId, Long accountId);
}
