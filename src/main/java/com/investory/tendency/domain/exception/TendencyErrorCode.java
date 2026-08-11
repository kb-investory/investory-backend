package com.investory.tendency.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum TendencyErrorCode implements ErrorCode {
    INSUFFICIENT_TRADE_DATA(ErrorType.UNPROCESSABLE, "TDC_001", "성향 분석을 진행하기에 거래 데이터가 부족합니다."),
    INSUFFICIENT_HOLDING_DATA(ErrorType.UNPROCESSABLE, "TDC_002", "위험배분 분석을 진행하기에 보유 종목 데이터가 부족합니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    TendencyErrorCode(ErrorType errorType, String code, String message) {
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
