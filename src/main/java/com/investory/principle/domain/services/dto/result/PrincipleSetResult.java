package com.investory.principle.domain.services.dto.result;

import com.investory.principle.domain.constant.PrincipleSetStatus;

import java.util.List;

public record PrincipleSetResult(
        Long principleSetId,
        int versionNo,
        PrincipleSetStatus setStatus,
        List<PrincipleItemResult> principles
) {
}
