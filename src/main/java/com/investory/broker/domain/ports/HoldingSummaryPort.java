package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;

// 계좌의 보유현황 요약(holding_snapshots 기반)을 ledger에 위임한다 —
// broker는 holding_snapshots를 소유하지 않으므로 직접 조회하지 않는다.
public interface HoldingSummaryPort {
    HoldingSummaryInfo summarize(Long userId, Long accountId);
}
