package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.BrokerLoginResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.ports.dto.RawHoldingBatch;
import com.investory.broker.domain.ports.dto.RawTradeRecord;

import java.util.List;

// 증권사 데이터 소스(현재는 목 서버, 추후 LIVE 실증권사로 교체 가능)를 추상화한 경계.
// 구현체(infra/clients)가 raw JSON 파싱·페이지네이션·필드 매핑을 전부 처리해서 넘기므로,
// domain(BrokerConnectionService)은 외부 응답 포맷을 전혀 몰라도 된다.
public interface BrokerFeedPort {
    BrokerLoginResult login(String loginId, String password);

    List<RawAccountRecord> fetchAccounts(String accessToken, String orgCode);

    List<RawTradeRecord> fetchTrades(String accessToken, String accountNum);

    RawHoldingBatch fetchHoldings(String accessToken, String accountNum);
}
