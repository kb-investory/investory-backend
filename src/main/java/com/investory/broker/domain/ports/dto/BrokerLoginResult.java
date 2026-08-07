package com.investory.broker.domain.ports.dto;

// mockConnectionId는 목 서버가 client-id/secret 기반 연동에 발급하는 재사용 가능한 식별자다
// (Investory 자체 broker_connections.connection_id와는 다른 값). 한 번 저장해두면
// 재동기화 때도 다시 인증할 필요 없이 그대로 재사용한다.
public record BrokerLoginResult(
    String mockConnectionId,
    String orgCode,
    String orgName
) {
}
