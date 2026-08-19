package com.investory.principle.domain.services.dto.result;

public record PrincipleItemResult(
        Long principleSetItemId,
        Long recommendationId,
        String principleText,
        PrincipleOriginResult origin,
        int sortOrder
) {
}
