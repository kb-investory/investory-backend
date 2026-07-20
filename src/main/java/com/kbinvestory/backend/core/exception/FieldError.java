package com.kbinvestory.backend.core.exception;

public record FieldError(
    String field,     // "targetPrice"
    String code,       // "INVALID_INPUT" 정도의 간단한 식별자, 혹은 ErrorType 재사용
    String message      // "목표가는 0보다 커야 합니다."
) {}
