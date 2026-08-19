package com.investory.broker.domain.ports;

// ledger.domain.services.AccountDataCleanupService.deleteAccountData(Long)로 위임 예정.
// 증권사 연동 해지 시 계좌 하나가 소유한 ledger 데이터(trades/trade_matches/holding_snapshots,
// 그리고 그 거래에 달린 journal_trade_notes)를 전부 지운다.
public interface AccountDataCleanupPort {
    void deleteAccountData(Long accountId);
}
