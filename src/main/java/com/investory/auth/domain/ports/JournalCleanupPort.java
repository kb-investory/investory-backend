package com.investory.auth.domain.ports;

// journal.domain.services.JournalService.deleteAllJournals(Long)로 위임 예정.
// 계정 탈퇴 시 사용자의 투자일지(investment_journals)를 전부 지운다. 호출 시점에 그 사용자의
// 거래(및 journal_trade_notes)는 BrokerConnectionCleanupPort를 통해 이미 정리됐다고 가정한다.
public interface JournalCleanupPort {
    void deleteAllJournals(Long userId);
}
