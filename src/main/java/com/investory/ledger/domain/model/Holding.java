package com.investory.ledger.domain.model;

import com.investory.ledger.domain.exception.LedgerErrorCode;
import com.investory.ledger.domain.exception.LedgerException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

// 계좌 하나, 종목 하나에 대한 특정 기준일 보유현황 스냅샷.
// 계좌 간 합산은 이 모델의 책임이 아니라 조회를 조립하는 서비스 계층에서 처리한다.
@Getter
public class Holding {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Long accountId;
    private final Long securityId;
    private final BigDecimal quantity;
    private final BigDecimal averagePurchasePrice;
    private final BigDecimal currentPrice;
    private final LocalDate snapshotDate;

    private Holding(Long accountId, Long securityId, BigDecimal quantity,
                     BigDecimal averagePurchasePrice, BigDecimal currentPrice, LocalDate snapshotDate) {
        requireNonNull(accountId);
        requireNonNull(securityId);
        requireNonNull(quantity);
        requireNonNull(averagePurchasePrice);
        requireNonNull(currentPrice);
        requireNonNull(snapshotDate);

        this.accountId = accountId;
        this.securityId = securityId;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.currentPrice = currentPrice;
        this.snapshotDate = snapshotDate;
    }

    private static void requireNonNull(Object value) {
        if (value == null) {
            throw new LedgerException(LedgerErrorCode.INVALID_HOLDING_DATA);
        }
    }

    public static Holding of(Long accountId, Long securityId, BigDecimal quantity,
                              BigDecimal averagePurchasePrice, BigDecimal currentPrice, LocalDate snapshotDate) {
        return new Holding(accountId, securityId, quantity, averagePurchasePrice, currentPrice, snapshotDate);
    }

    public BigDecimal getPurchaseAmount() {
        return quantity.multiply(averagePurchasePrice);
    }

    public BigDecimal getMarketValue() {
        return quantity.multiply(currentPrice);
    }

    public BigDecimal getProfitLossAmount() {
        return getMarketValue().subtract(getPurchaseAmount());
    }

    // 매입금액이 0이면(전량 무상 취득 등 예외적인 케이스) 0% 처리
    public BigDecimal getReturnRate() {
        BigDecimal purchaseAmount = getPurchaseAmount();
        if (purchaseAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return getProfitLossAmount()
                .divide(purchaseAmount, MathContext.DECIMAL64)
                .multiply(HUNDRED);
    }
}
