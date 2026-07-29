package com.investory.core.exception;

public record FieldError(
    String field,     // "targetPrice"
    String message      // "목표가는 0보다 커야 합니다."
) {}
