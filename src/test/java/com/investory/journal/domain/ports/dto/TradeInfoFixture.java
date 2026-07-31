package com.investory.journal.domain.ports.dto;

import com.investory.journal.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public class TradeInfoFixture {

    public static TradeInfo trade(Long tradeId, Long securityId, TradeSide tradeSide, Instant tradedAt) {
        return new TradeInfo(tradeId, securityId, tradeSide, 10, new BigDecimal("72000"), tradedAt);
    }
}
