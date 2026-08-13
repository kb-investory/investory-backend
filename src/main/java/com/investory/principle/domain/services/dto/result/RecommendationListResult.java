package com.investory.principle.domain.services.dto.result;

import java.util.List;

public record RecommendationListResult(
        List<RecommendationResult> recommendations
) {
}
