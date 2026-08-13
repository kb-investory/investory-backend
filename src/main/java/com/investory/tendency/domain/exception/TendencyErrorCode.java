package com.investory.tendency.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum TendencyErrorCode implements ErrorCode {
    INSUFFICIENT_TRADE_DATA(ErrorType.UNPROCESSABLE, "TDC_001", "성향 분석을 진행하기에 거래 데이터가 부족합니다."),
    INSUFFICIENT_HOLDING_DATA(ErrorType.UNPROCESSABLE, "TDC_002", "위험배분 분석을 진행하기에 보유 종목 데이터가 부족합니다."),
    ANALYSIS_RUN_NOT_FOUND(ErrorType.NOT_FOUND, "TDC_003", "존재하지 않거나 본인 소유가 아닌 분석 실행입니다.");

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
