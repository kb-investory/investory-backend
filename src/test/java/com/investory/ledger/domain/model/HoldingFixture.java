package com.investory.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HoldingFixture {

    public static Holding holding(Long accountId, Long securityId, BigDecimal quantity,
                                   BigDecimal averagePurchasePrice, BigDecimal currentPrice, LocalDate snapshotDate) {
        return Holding.of(accountId, securityId, quantity, averagePurchasePrice, currentPrice, snapshotDate);
    }
}
