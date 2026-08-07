package com.investory.broker.domain.ports.dto;

import java.util.List;

public record AccountHoldingsInfo(
    HoldingSummaryInfo summary,
    List<HoldingDetailInfo> holdings
) {
}
