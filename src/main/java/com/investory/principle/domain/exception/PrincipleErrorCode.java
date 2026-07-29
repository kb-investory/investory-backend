package com.investory.principle.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum PrincipleErrorCode implements ErrorCode {
    PRINCIPLE_CONFLICT(ErrorType.CONFLICT, "PRN_001", "기존 원칙과 충돌합니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    PrincipleErrorCode(ErrorType errorType, String code, String message) {
        this.errorType = errorType;
        this.code = code;
        this.message = message;
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
