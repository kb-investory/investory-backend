package com.investory.ledger.domain.services.dto.command;

import java.util.List;

public record IngestRawTradesCommand(
    Long userId,
    Long accountId,
    List<RawTradeRecord> rawTrades
) {
}
