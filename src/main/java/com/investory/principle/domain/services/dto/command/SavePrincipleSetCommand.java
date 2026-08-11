package com.investory.principle.domain.services.dto.command;

import java.util.List;

public record SavePrincipleSetCommand(
        Long userId,
        Long analysisRunId,
        List<PrincipleItemCommand> principles
) {
}
