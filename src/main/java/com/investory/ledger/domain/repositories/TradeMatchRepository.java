package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.TradeMatch;

import java.time.Instant;
import java.util.List;

public interface TradeMatchRepository {

    // 재계산 전 해당 계좌·종목의 기존 매칭을 전부 지운다 (과거 거래 소급 삽입에도 정확성 보장)
    void deleteByAccountIdAndSecurityId(Long accountId, Long securityId);

    // 증권사 연동 해지 시 이 계좌의 매칭을 종목 구분 없이 전부 지운다.
    void deleteByAccountId(Long accountId);

    void saveAll(List<TradeMatch> matches);

    // 지정한 계좌들의 매칭 중 created_at이 since 이후인 것들의 holding_days만 조회한다.
    // holding_days는 매칭 시점에 이미 계산되어 저장된 값을 그대로 반환한다 — 여기서 다시 계산하지 않는다.
    List<Integer> findHoldingDaysByAccountIdsSince(List<Long> accountIds, Instant since);
}
