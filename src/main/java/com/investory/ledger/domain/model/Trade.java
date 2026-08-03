package com.investory.ledger.domain.model;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.exception.LedgerErrorCode;
import com.investory.ledger.domain.exception.LedgerException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class Trade {

    private final Long tradeId;
    private final Long accountId;
    private final Long securityId;
    private final TradeSide tradeSide;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal transactionCostAmount;
    private final String externalTradeId;
    private final Instant tradedAt;

    private Trade(Long tradeId, Long accountId, Long securityId, TradeSide tradeSide,
                   BigDecimal quantity, BigDecimal unitPrice, BigDecimal transactionCostAmount,
                   String externalTradeId, Instant tradedAt) {
        requireNonNull(accountId);
        requireNonNull(securityId);
        requireNonNull(tradeSide);
        requireNonNull(quantity);
        requireNonNull(unitPrice);
        requireNonNull(transactionCostAmount);
        requireNonNull(externalTradeId);
        requireNonNull(tradedAt);

        this.tradeId = tradeId;
        this.accountId = accountId;
        this.securityId = securityId;
        this.tradeSide = tradeSide;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.transactionCostAmount = transactionCostAmount;
        this.externalTradeId = externalTradeId;
        this.tradedAt = tradedAt;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new LedgerException(LedgerErrorCode.INVALID_TRADE_DATA);
        }
    }

    // 신규 적재: tradeId는 아직 없고(DB가 생성)
    public static Trade create(Long accountId, Long securityId, TradeSide tradeSide,
                                BigDecimal quantity, BigDecimal unitPrice, BigDecimal transactionCostAmount,
                                String externalTradeId, Instant tradedAt) {
        return new Trade(null, accountId, securityId, tradeSide, quantity, unitPrice,
                transactionCostAmount, externalTradeId, tradedAt);
    }

    // 영속화된 데이터로부터 복원 (매퍼 등에서 사용)
    public static Trade of(Long tradeId, Long accountId, Long securityId, TradeSide tradeSide,
                            BigDecimal quantity, BigDecimal unitPrice, BigDecimal transactionCostAmount,
                            String externalTradeId, Instant tradedAt) {
        return new Trade(tradeId, accountId, securityId, tradeSide, quantity, unitPrice,
                transactionCostAmount, externalTradeId, tradedAt);
    }

    public BigDecimal getTradeAmount() {
        return quantity.multiply(unitPrice);
    }

    // 매수: 거래금액 + 거래비용, 매도: 거래금액 - 거래비용
    public BigDecimal getSettlementAmount() {
        BigDecimal tradeAmount = getTradeAmount();
        return tradeSide == TradeSide.BUY
                ? tradeAmount.add(transactionCostAmount)
                : tradeAmount.subtract(transactionCostAmount);
    }
}
