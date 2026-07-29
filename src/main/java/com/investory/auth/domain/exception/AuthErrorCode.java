package com.investory.auth.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum AuthErrorCode implements ErrorCode {
    OAUTH_CODE_INVALID(ErrorType.UNAUTHORIZED, "AUTH_001", "유효하지 않은 OAuth 인가 코드입니다."),
    TOKEN_EXPIRED(ErrorType.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다."),
    TOKEN_INVALID(ErrorType.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    AuthErrorCode(ErrorType errorType, String code, String message) {
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
