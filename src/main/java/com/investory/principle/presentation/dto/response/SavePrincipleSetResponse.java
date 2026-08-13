package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.SavePrincipleSetResult;

public record SavePrincipleSetResponse(
        Long principleSetId,
        int versionNo,
        String setStatus,
        String message
) {
    public static SavePrincipleSetResponse from(SavePrincipleSetResult result) {
        return new SavePrincipleSetResponse(result.principleSetId(), result.versionNo(), result.setStatus().name(), result.message());
    }
}
