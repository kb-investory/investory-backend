package com.kbinvestory.backend.account.domain.services.dto.command;

public record CreateBrokerConnectionCommand(
    Long userId,
    Long providerId,
    String loginId,
    String password
) {}