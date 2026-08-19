package com.investory.ledger.domain.ports;

import com.investory.ledger.domain.ports.dto.SecurityInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MarketDataPort {

    // 조회 응답 조립용 — securityId로 표시 정보 일괄 조회
    List<SecurityInfo> findSecurities(List<Long> securityIds);

    // 적재(ingestion) 시점에 broker가 넘겨준 종목코드를 우리 securityId로 해석
    Optional<SecurityInfo> resolveByCode(String securityCode);

    // resolveByCode의 배치 버전 — 거래/보유 적재 시 원시 레코드 개수만큼 반복 조회하던 것을 한 번에 처리한다.
    // securityCode로 찾은 것만 담아 반환한다(못 찾은 코드는 결과 맵에 아예 없음).
    Map<String, SecurityInfo> resolveByCodes(List<String> securityCodes);
}
