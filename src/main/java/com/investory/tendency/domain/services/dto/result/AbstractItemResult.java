package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.PrincipleComplianceGrade;

public record AbstractItemResult(
    Long principleItemId,
    String principleText,
    PrincipleComplianceGrade grade
) {
}
