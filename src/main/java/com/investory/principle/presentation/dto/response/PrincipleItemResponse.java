package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.PrincipleItemResult;

public record PrincipleItemResponse(
        Long principleSetItemId,
        String principleText,
        PrincipleOriginResponse origin,
        int sortOrder
) {
    public static PrincipleItemResponse from(PrincipleItemResult result) {
        return new PrincipleItemResponse(
                result.principleSetItemId(), result.principleText(), PrincipleOriginResponse.from(result.origin()), result.sortOrder());
    }
}
