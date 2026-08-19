package com.investory.ledger.domain.repositories;

import com.investory.ledger.domain.model.Holding;

import java.util.List;

public interface HoldingSnapshotRepository {

    // accountIds 각각에 대해 계좌·종목 조합별 "가장 최근 snapshot_date" 행만 반환.
    // securityId가 null이면 전체 종목.
    List<Holding> findLatestByAccountIds(List<Long> accountIds, Long securityId);

    // (accountId, securityId, snapshotDate) 기준 upsert
    void upsert(Holding holding);

    // 증권사 연동 해지 시 이 계좌의 보유 스냅샷을 전부 지운다.
    void deleteByAccountId(Long accountId);
}
