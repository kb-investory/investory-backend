package com.investory.principle.presentation.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.investory.principle.domain.services.dto.command.PrincipleItemCommand;

public record PrincipleItemRequest(
        Long recommendationId,
        String principleText,
        JsonNode ruleJson,
        int sortOrder
) {
    public PrincipleItemCommand toCommand() {
        return new PrincipleItemCommand(recommendationId, principleText, ruleJson == null ? null : ruleJson.toString(), sortOrder);
    }
}
