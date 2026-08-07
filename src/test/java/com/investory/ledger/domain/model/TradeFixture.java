package com.investory.ledger.domain.model;

import com.investory.ledger.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public class TradeFixture {

    public static Trade trade(Long accountId, Long securityId, TradeSide tradeSide, String externalTradeId, Instant tradedAt) {
        return Trade.create(accountId, securityId, tradeSide, BigDecimal.TEN, BigDecimal.valueOf(10000),
                BigDecimal.valueOf(100), externalTradeId, tradedAt);
    }
}
