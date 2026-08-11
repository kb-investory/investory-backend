package com.investory.principle.domain.services.dto.command;

public record PrincipleItemCommand(
        Long recommendationId,
        String principleText,
        String ruleJson,
        int sortOrder
) {
}
