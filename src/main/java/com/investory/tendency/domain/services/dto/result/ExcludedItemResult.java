package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.PrincipleExclusionReason;

public record ExcludedItemResult(
    Long principleItemId,
    String principleText,
    PrincipleExclusionReason reason
) {
}
