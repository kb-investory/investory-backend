package com.investory.principle.presentation.dto.response;

import com.investory.principle.domain.services.dto.result.PrincipleSetResult;

import java.util.List;
import java.util.stream.Collectors;

public record PrincipleSetResponse(
        Long principleSetId,
        int versionNo,
        String setStatus,
        List<PrincipleItemResponse> principles
) {
    public static PrincipleSetResponse from(PrincipleSetResult result) {
        List<PrincipleItemResponse> principles = result.principles().stream()
                .map(PrincipleItemResponse::from)
                .collect(Collectors.toList());
        return new PrincipleSetResponse(result.principleSetId(), result.versionNo(), result.setStatus().name(), principles);
    }
}
