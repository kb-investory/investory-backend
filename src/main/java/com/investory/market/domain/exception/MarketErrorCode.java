package com.investory.market.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum MarketErrorCode implements ErrorCode {
    STOCK_NOT_FOUND(ErrorType.NOT_FOUND, "MKT_001", "존재하지 않는 종목입니다."),
    INVALID_STOCK_DATA(ErrorType.INVALID_INPUT, "MKT_002", "종목 정보가 올바르지 않습니다."),
    KIS_API_ERROR(ErrorType.EXTERNAL_ERROR, "MKT_003", "한국투자증권 API 연동 중 오류가 발생했습니다."),
    STOCK_PRICE_NOT_FOUND(ErrorType.NOT_FOUND, "MKT_004", "해당 날짜의 시세 정보가 없습니다."),
    INVALID_MARKET_TYPE(ErrorType.INVALID_INPUT, "MKT_005", "marketType은 KOSPI, KOSDAQ, KONEX 중 하나여야 합니다.");

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
