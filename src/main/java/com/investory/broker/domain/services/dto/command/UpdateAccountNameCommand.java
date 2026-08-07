package com.investory.broker.domain.services.dto.command;

public record UpdateAccountNameCommand(
    Long userId,
    Long accountId,
    String accountName
) {
}
