package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.PrincipleOriginResult;

public record PrincipleOriginResponse(
        String type,
        String analysisTypeName
) {
    public static PrincipleOriginResponse from(PrincipleOriginResult result) {
        return new PrincipleOriginResponse(result.type().name(), result.analysisTypeName());
    }
}
