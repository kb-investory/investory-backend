package com.investory.auth.infra.exception;


import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum AuthInfraErrorCode implements ErrorCode {
    USER_QUERY_FAILED(ErrorType.INTERNAL_ERROR, "AUTHINFRA_001", "회원 조회 중 오류가 발생했습니다."),
    USER_SAVE_FAILED(ErrorType.INTERNAL_ERROR, "AUTHINFRA_002", "회원 저장 중 오류가 발생했습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    AuthInfraErrorCode(ErrorType errorType, String code, String message) {
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
