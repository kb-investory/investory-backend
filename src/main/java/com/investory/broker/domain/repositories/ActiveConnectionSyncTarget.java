package com.investory.broker.domain.repositories;

// BrokerAccountSyncScheduler 전용 조회 결과 — 사용자 소유권 판단 없이 시스템이 전체 연결을 순회할 때만
// 쓰인다. mockProfileCode는 일반 BrokerConnection 모델에는 없는 인증 정보라(§5 principles) 별도 타입으로 둔다.
public record ActiveConnectionSyncTarget(
        Long userId,
        Long connectionId,
        String mockProfileCode,
        String brokerCode
) {
}
