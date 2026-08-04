package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.BrokerLoginResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.ports.dto.RawHoldingBatch;
import com.investory.broker.domain.ports.dto.RawTradeRecord;

import java.util.List;

// 증권사 데이터 소스(현재는 목 서버, 추후 LIVE 실증권사로 교체 가능)를 추상화한 경계.
// 구현체(infra/clients)가 raw JSON 파싱·페이지네이션·필드 매핑을 전부 처리해서 넘기므로,
// domain(BrokerConnectionService)은 외부 응답 포맷을 전혀 몰라도 된다.
//
// 인증은 client-id/secret + connectionId 방식이다: login()으로 최초 1회 connectionId를 발급받아
// 저장해두면, 이후 데이터 조회는 그 connectionId만으로 계속 가능하다(비밀번호 재요구 없음,
// 별도 재인증 단계도 없음) — fetchXxx 메서드들이 받는 mockConnectionId가 바로 그 값이다.
public interface BrokerFeedPort {
    BrokerLoginResult login(String loginId, String password);

    List<RawAccountRecord> fetchAccounts(String mockConnectionId, String orgCode);

    List<RawTradeRecord> fetchTrades(String mockConnectionId, String accountNum);

    RawHoldingBatch fetchHoldings(String mockConnectionId, String accountNum);
}
