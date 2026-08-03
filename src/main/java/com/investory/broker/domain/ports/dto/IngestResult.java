package com.investory.broker.domain.ports.dto;

import java.util.List;

public record IngestResult(
    int successCount,
    int skippedCount,
    List<String> skippedReasons
) {
}
