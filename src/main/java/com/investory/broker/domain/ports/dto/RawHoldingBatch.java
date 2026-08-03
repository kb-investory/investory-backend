package com.investory.broker.domain.ports.dto;

import java.time.LocalDate;
import java.util.List;

public record RawHoldingBatch(
    LocalDate baseDate,
    List<RawHoldingRecord> holdings
) {
}
