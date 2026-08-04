package com.investory.ledger.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

// 매수-매도 FIFO 매칭 결과 (파생 데이터). 재계산 시 항상 전체 삭제 후 재생성되므로
// 개별 행을 참조할 ID를 domain에서 다루지 않는다 (trade_match_id는 DB 전용).
@Getter
public class TradeMatch {

    private final Long buyTradeId;
    private final Long sellTradeId;
    private final Long securityId;
    private final BigDecimal matchedQuantity;
    private final BigDecimal buyPrice;
    private final BigDecimal sellPrice;
    private final BigDecimal realizedPnl;
    private final BigDecimal returnRate;
    private final long holdingDays;

    private TradeMatch(Long buyTradeId, Long sellTradeId, Long securityId, BigDecimal matchedQuantity,
                        BigDecimal buyPrice, BigDecimal sellPrice, BigDecimal realizedPnl,
                        BigDecimal returnRate, long holdingDays) {
        this.buyTradeId = buyTradeId;
        this.sellTradeId = sellTradeId;
        this.securityId = securityId;
        this.matchedQuantity = matchedQuantity;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.realizedPnl = realizedPnl;
        this.returnRate = returnRate;
        this.holdingDays = holdingDays;
    }

    public static TradeMatch of(Long buyTradeId, Long sellTradeId, Long securityId, BigDecimal matchedQuantity,
                                 BigDecimal buyPrice, BigDecimal sellPrice, BigDecimal realizedPnl,
                                 BigDecimal returnRate, long holdingDays) {
        return new TradeMatch(buyTradeId, sellTradeId, securityId, matchedQuantity, buyPrice, sellPrice,
                realizedPnl, returnRate, holdingDays);
    }
}
