package com.kbinvestory.backend.account.domain.exception;

import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.ErrorType;

public enum AccountErrorCode implements ErrorCode {
    PROVIDER_NOT_FOUND(ErrorType.NOT_FOUND, "ACNT_001", "존재하지 않는 증권사입니다."),
    ALREADY_CONNECTED(ErrorType.CONFLICT, "ACNT_002", "이미 연동된 증권사입니다."),
    BROKER_AUTH_FAILED(ErrorType.EXTERNAL_ERROR, "ACNT_003", "증권사 인증에 실패했습니다. 아이디 또는 비밀번호를 확인해주세요."),
    INVALID_CONNECTION_DATA(ErrorType.INVALID_INPUT, "ACNT_004", "계좌 연동 정보가 올바르지 않습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    AccountErrorCode(ErrorType errorType, String code, String message) {
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
