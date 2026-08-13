package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.TradeInfo;

import java.util.List;

public interface TradeLedgerPort {

    // 특정 유저의 특정 종목 전체 매매 이력(모든 계좌 합산), 매매일시 오름차순.
    // 평단은 분석창(예: 90일)과 무관하게 첫 매수부터 전체 누적으로 계산해야 하므로 기간 제한 없이 전체를 받는다.
    List<TradeInfo> findTrades(Long userId, Long securityId);

    // 유저의 전체 종목·전체 계좌 매매 이력, 매매일시 오름차순, 기간 제한 없음.
    // 6번(원칙 이행)이 "어떤 종목을 평가 대상으로 삼을지"와 포트폴리오 요약 통계를 뽑는 용도.
    // 개별 종목의 평단 계산에는 여전히 findTrades(userId, securityId)를 쓴다.
    List<TradeInfo> findAllTrades(Long userId);
}
