package com.investory.ledger.infra.entities;

import com.investory.ledger.domain.model.TradeMatch;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TradeMatchRow {
    private Long accountId;
    private Long securityId;
    private Long buyTradeId;
    private Long sellTradeId;
    private BigDecimal matchedQuantity;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal realizedPnl;
    private BigDecimal returnRate;
    private int holdingDays;

    public TradeMatch toDomain() {
        return TradeMatch.of(accountId, buyTradeId, sellTradeId, securityId, matchedQuantity, buyPrice, sellPrice,
                realizedPnl, returnRate, holdingDays);
    }

    public static TradeMatchRow from(TradeMatch match) {
        TradeMatchRow row = new TradeMatchRow();
        row.accountId = match.getAccountId();
        row.securityId = match.getSecurityId();
        row.buyTradeId = match.getBuyTradeId();
        row.sellTradeId = match.getSellTradeId();
        row.matchedQuantity = match.getMatchedQuantity();
        row.buyPrice = match.getBuyPrice();
        row.sellPrice = match.getSellPrice();
        row.realizedPnl = match.getRealizedPnl();
        row.returnRate = match.getReturnRate();
        row.holdingDays = (int) match.getHoldingDays();
        return row;
    }
}
