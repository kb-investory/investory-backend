package com.investory.tendency.domain.ports.dto;

import java.math.BigDecimal;
import java.time.Instant;

// ledger의 거래 한 건. tradeSide는 ledger의 TradeSide enum을 그대로 import하지 않고
// "BUY"/"SELL" 문자열로 받는다 (broker.domain.ports.dto.RawTradeRecord와 동일한 패턴 —
// 각 도메인은 상대 도메인의 enum이 아니라 자기 로컬 표현을 갖는다).
public record TradeInfo(
    Long securityId,
    String tradeSide,
    BigDecimal quantity,
    BigDecimal unitPrice,
    Instant tradedAt
) {
}
