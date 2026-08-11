package com.investory.tendency.domain.ports;

import java.util.List;

// ledger 도메인이 소유한 trade_matches 데이터를 tendency 쪽에서 필요로 하는 모양대로 요청하는 포트.
// 구현체(TradeMatchQueryPortImpl)는 ledger.domain.repositories.TradeMatchRepository와
// ledger.domain.ports.AccountPort를 통해서만 데이터를 받아온다 — ledger의 테이블/SQL을 직접 알지 않는다.
public interface TradeMatchQueryPort {

    // 지정한 사용자의 최근 90일 trade_matches의 holding_days 목록을 반환한다.
    // holding_days는 매칭 시점에 이미 계산되어 저장된 값을 그대로 반환한다 — 여기서 다시 계산하지 않는다.
    List<Integer> findHoldingDaysForLast90Days(Long userId);
}
