package com.investory.broker.domain.ports.dto;

// mydata account_type은 원시 코드("101" 등) 그대로 넘긴다 — STOCK/ISA 등 도메인 enum으로의
// 해석은 domain(BrokerConnectionService)의 책임이지, feed 쪽(infra)이 알 개념이 아니다.
public record RawAccountRecord(
    String accountNum,
    String accountName,
    String accountType,
    String currencyCode
) {
}
