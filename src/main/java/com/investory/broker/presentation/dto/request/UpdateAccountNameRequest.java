package com.investory.broker.presentation.dto.request;

import com.investory.broker.domain.services.dto.command.UpdateAccountNameCommand;

public record UpdateAccountNameRequest(
    String accountName
) {
    public UpdateAccountNameCommand toCommand(Long userId, Long accountId) {
        return new UpdateAccountNameCommand(userId, accountId, accountName);
    }
}
