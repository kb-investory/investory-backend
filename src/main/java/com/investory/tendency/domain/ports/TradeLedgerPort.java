package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.TradeInfo;

import java.util.List;

public interface TradeLedgerPort {

    // 특정 유저의 특정 종목 전체 매매 이력(모든 계좌 합산), 매매일시 오름차순.
    // 평단은 분석창(예: 90일)과 무관하게 첫 매수부터 전체 누적으로 계산해야 하므로 기간 제한 없이 전체를 받는다.
    List<TradeInfo> findTrades(Long userId, Long securityId);
}
