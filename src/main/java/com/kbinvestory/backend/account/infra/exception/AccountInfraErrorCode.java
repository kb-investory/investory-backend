package com.kbinvestory.backend.account.infra.exception;

import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.ErrorType;

public enum AccountInfraErrorCode implements ErrorCode {
    BROKERAGE_PROVIDER_QUERY_FAILED(ErrorType.INTERNAL_ERROR, "ACNT_INFRA_001", "증권사 목록을 조회하는 중 오류가 발생했습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    AccountInfraErrorCode(ErrorType errorType, String code, String message) {
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