package com.investory.journal.domain.ports.dto;

import com.investory.journal.domain.constant.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;

public class TradeTimelineInfoFixture {

    public static TradeTimelineInfo trade(Long tradeId, TradeSide tradeSide, Instant tradedAt) {
        return new TradeTimelineInfo(tradeId, 25L, "종합주식계좌", tradeSide, 10, new BigDecimal("72000"), new BigDecimal("150"), tradedAt);
    }
}
