package com.investory.auth.domain.ports;

// broker.domain.services.BrokerConnectionService.disconnectConnection(...)을 사용자의 모든 연결에
// 반복 호출하는 방식으로 위임 예정. 계정 탈퇴 시 사용자의 모든 증권사 연동을 해지한다 —
// 그 결과로 계좌/거래/보유(ledger)와 그 거래에 달린 매매 근거(journal)까지 함께 정리된다.
public interface BrokerConnectionCleanupPort {
    void disconnectAllConnections(Long userId);
}
