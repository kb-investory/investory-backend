package com.kbinvestory.backend.account.domain.ports.dto;

public record BrokerAuthInfo(
    boolean success,
    String connectedId,
    String failureReason
) {}
