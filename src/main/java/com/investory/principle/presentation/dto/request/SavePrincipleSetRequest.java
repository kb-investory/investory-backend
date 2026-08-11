package com.investory.principle.presentation.dto.request;

import com.investory.principle.domain.services.dto.command.PrincipleItemCommand;
import com.investory.principle.domain.services.dto.command.SavePrincipleSetCommand;

import java.util.List;
import java.util.stream.Collectors;

public record SavePrincipleSetRequest(
        Long analysisRunId,
        List<PrincipleItemRequest> principles
) {
    public SavePrincipleSetCommand toCommand(Long userId) {
        List<PrincipleItemCommand> items = principles == null ? List.of() : principles.stream()
                .map(PrincipleItemRequest::toCommand)
                .collect(Collectors.toList());
        return new SavePrincipleSetCommand(userId, analysisRunId, items);
    }
}
