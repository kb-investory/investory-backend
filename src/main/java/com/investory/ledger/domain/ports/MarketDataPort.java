package com.investory.ledger.domain.ports;

import com.investory.ledger.domain.ports.dto.SecurityInfo;

import java.util.List;
import java.util.Optional;

public interface MarketDataPort {

    // 조회 응답 조립용 — securityId로 표시 정보 일괄 조회
    List<SecurityInfo> findSecurities(List<Long> securityIds);

    // 적재(ingestion) 시점에 broker가 넘겨준 종목코드를 우리 securityId로 해석
    Optional<SecurityInfo> resolveByCode(String securityCode);
}
