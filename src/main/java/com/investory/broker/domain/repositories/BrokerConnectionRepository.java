package com.investory.broker.domain.repositories;

import com.investory.broker.domain.constant.ConnectionStatus;
import com.investory.broker.domain.model.BrokerConnection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BrokerConnectionRepository {
    // DISCONNECTED는 제외한다 — 해지된 연결은 이력으로 DB에 남지만 "내 연동 목록"에는 노출하지 않는다.
    List<BrokerConnection> findAllByUserId(Long userId);

    Optional<BrokerConnection> findActiveByUserIdAndBrokerId(Long userId, Long brokerId);

    Optional<BrokerConnection> findByIdAndUserId(Long connectionId, Long userId);

    // 소유권 확인 없이 표시 정보 조립용으로만 사용 (예: 다른 도메인의 Port에 계좌 목록을 넘겨줄 때)
    List<BrokerConnection> findByIds(List<Long> connectionIds);

    // 재동기화용 — 목 서버 재인증에 필요한 loginId(mock_profile_code에 저장됨)를 가져온다.
    // 호출 전에 findByIdAndUserId로 소유권을 이미 확인했다고 가정한다.
    Optional<String> findMockProfileCodeByConnectionId(Long connectionId);

    // BrokerAccountSyncScheduler 전용 — 사용자 구분 없이 CONNECTED 상태인 모든 연결의 배치 동기화 재료를 반환한다.
    List<ActiveConnectionSyncTarget> findAllActiveForSync();

    Long insert(Long userId, Long brokerId, String mockProfileCode, Instant connectedAt);

    void updateLastSyncedAt(Long connectionId, Instant lastSyncedAt);

    void updateStatus(Long connectionId, ConnectionStatus status);
}