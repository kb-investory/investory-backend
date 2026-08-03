package com.investory.ledger.domain.services.dto.result;

import java.util.List;

public record IngestResult(
    int successCount,
    int skippedCount,
    List<String> skippedReasons
) {
}
