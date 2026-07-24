package com.kbinvestory.backend.account.infra.exception;

import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.ErrorType;

public enum AccountInfraErrorCode implements ErrorCode {
    BROKERAGE_PROVIDER_QUERY_FAILED(ErrorType.INTERNAL_ERROR, "ACNT_INFRA_001", "증권사 목록을 조회하는 중 오류가 발생했습니다."),
    CODEF_REQUEST_FAILED(ErrorType.EXTERNAL_ERROR, "ACNT_INFRA_002", "증권사 인증 요청 중 오류가 발생했습니다."),
    CODEF_RESPONSE_PARSE_FAILED(ErrorType.EXTERNAL_ERROR, "ACNT_INFRA_003", "증권사 인증 응답 처리 중 오류가 발생했습니다."),
    ACCOUNT_CONNECTION_QUERY_FAILED(ErrorType.INTERNAL_ERROR, "ACNT_INFRA_004", "계좌 연동 정보를 조회하는 중 오류가 발생했습니다."),
    ACCOUNT_CONNECTION_SAVE_FAILED(ErrorType.INTERNAL_ERROR, "ACNT_INFRA_005", "계좌 연동 정보를 저장하는 중 오류가 발생했습니다.");

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