package com.investory.principle.domain.services.dto.result;

import com.investory.principle.domain.constant.PrincipleSetStatus;

public record SavePrincipleSetResult(
        Long principleSetId,
        int versionNo,
        PrincipleSetStatus setStatus,
        String message
) {
}
