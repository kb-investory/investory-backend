package com.investory.principle.domain.services.dto.result;

import com.investory.principle.domain.constant.PrincipleOriginType;

public record PrincipleOriginResult(
        PrincipleOriginType type,
        String analysisTypeName
) {
}
