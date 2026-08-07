package com.investory.auth.domain.services.dto.result;

public record LoginResult(Long userId, String accessToken, String refreshToken, boolean newUser) {
}
