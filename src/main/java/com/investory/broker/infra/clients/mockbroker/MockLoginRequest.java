package com.investory.broker.infra.clients.mockbroker;

public record MockLoginRequest(
    String loginId,
    String loginPassword
) {
}
