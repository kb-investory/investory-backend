package com.kbinvestory.backend.market.domain.exception;

import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.ErrorType;

public enum MarketErrorCode implements ErrorCode {
    STOCK_NOT_FOUND(ErrorType.NOT_FOUND, "MAKT_001", "존재하지 않는 종목입니다."),
    INVALID_STOCK_DATA(ErrorType.INVALID_INPUT, "MAKT_002", "종목 정보가 올바르지 않습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    MarketErrorCode(ErrorType errorType, String code, String message) {
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