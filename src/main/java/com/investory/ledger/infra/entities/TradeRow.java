package com.investory.ledger.infra.entities;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.Trade;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class TradeRow {
    private Long tradeId;
    private Long accountId;
    private Long securityId;
    private TradeSide tradeSide;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal transactionCostAmount;
    private String externalTradeId;
    private Instant tradedAt;

    public Trade toDomain() {
        return Trade.of(tradeId, accountId, securityId, tradeSide, quantity, unitPrice,
                transactionCostAmount, externalTradeId, tradedAt);
    }

    public static TradeRow from(Trade trade) {
        TradeRow row = new TradeRow();
        row.tradeId = trade.getTradeId();
        row.accountId = trade.getAccountId();
        row.securityId = trade.getSecurityId();
        row.tradeSide = trade.getTradeSide();
        row.quantity = trade.getQuantity();
        row.unitPrice = trade.getUnitPrice();
        row.transactionCostAmount = trade.getTransactionCostAmount();
        row.externalTradeId = trade.getExternalTradeId();
        row.tradedAt = trade.getTradedAt();
        return row;
    }
}
