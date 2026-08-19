package com.investory.ledger.domain.events;

// ledger가 계좌 하나에 새 거래를 적재했을 때 발행하는 이벤트. ledger -> TradesIngestedEvent -> notification.
// 매수·매도 매칭 재계산(CLAUDE.md §8-2의 "asset 내부" 행)은 같은 도메인이라 이벤트가 아니라
// TradeIngestionService가 TradeMatchingService를 직접 호출해서 처리한다 — 이 이벤트는 오직
// notification으로의 크로스 도메인 전달용이다.
//
// 현재 코드는 아직 account -> asset 이관 전이라 ledger가 trades를 소유하므로 여기 둔다.
// 나중에 asset으로 병합되면 asset.domain.events로 옮긴다.
public record TradesIngestedEvent(
        Long userId,
        Long accountId,
        int insertedTradeCount
) {
}
