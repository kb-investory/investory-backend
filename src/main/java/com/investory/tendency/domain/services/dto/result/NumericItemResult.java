package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.PrincipleRuleType;

import java.math.BigDecimal;

public record NumericItemResult(
    Long principleItemId,
    String principleText,
    PrincipleRuleType type,
    BigDecimal thresholdPercent,
    int opportunities,
    int compliance
) {
}
