package com.investory.auth.domain.ports.dto;

public record TokenPair(String accessToken, String refreshToken) {
}
