package com.investory.ledger.infra.entities;

import com.investory.ledger.domain.model.Holding;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

// holding_snapshots 테이블은 current_price를 따로 저장하지 않고 market_value(=quantity*currentPrice)만
// 저장한다 — 도메인 Holding 모델은 currentPrice를 필드로 갖고 있어, 이 변환 레이어에서
// 곱셈(저장)/나눗셈(복원)으로 흡수한다.
@Getter
@Setter
@NoArgsConstructor
public class HoldingSnapshotRow {
    private Long accountId;
    private Long securityId;
    private LocalDate snapshotDate;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private BigDecimal marketValue;
    private BigDecimal unrealizedPnl;

    public Holding toDomain() {
        BigDecimal currentPrice = quantity.signum() == 0
                ? BigDecimal.ZERO
                : marketValue.divide(quantity, MathContext.DECIMAL64);
        return Holding.of(accountId, securityId, quantity, averageCost, currentPrice, snapshotDate);
    }

    public static HoldingSnapshotRow from(Holding holding) {
        HoldingSnapshotRow row = new HoldingSnapshotRow();
        row.accountId = holding.getAccountId();
        row.securityId = holding.getSecurityId();
        row.snapshotDate = holding.getSnapshotDate();
        row.quantity = holding.getQuantity();
        row.averageCost = holding.getAveragePurchasePrice();
        row.marketValue = holding.getMarketValue();
        row.unrealizedPnl = holding.getProfitLossAmount();
        return row;
    }
}
