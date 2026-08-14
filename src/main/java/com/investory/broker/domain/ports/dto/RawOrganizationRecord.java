package com.investory.broker.domain.ports.dto;

// 목 증권사 서버가 관리하는 기관(org) 원시값. 우리 DB의 broker_providers와는 별개로,
// 목 서버 쪽 진실 원본(org_code/org_name)을 그대로 옮겨온 것 — 도메인 모델(BrokerProvider)로의
// 해석(upsert 여부 판단 등)은 domain(BrokerProviderService)의 책임이다.
public record RawOrganizationRecord(
    String orgCode,
    String orgName
) {
}
