package com.investory.market.infra.exception;

import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.ErrorType;

public enum MarketInfraErrorCode implements ErrorCode {
    STOCK_QUERY_FAILED(ErrorType.INTERNAL_ERROR, "MKT_INFRA_001", "종목 조회 중 오류가 발생했습니다."),
    STOCK_SAVE_FAILED(ErrorType.INTERNAL_ERROR, "MKT_INFRA_002", "종목 저장 중 오류가 발생했습니다."),
    STOCK_PRICE_QUERY_FAILED(ErrorType.INTERNAL_ERROR, "MKT_INFRA_003", "시세 조회 중 오류가 발생했습니다."),
    STOCK_PRICE_SAVE_FAILED(ErrorType.INTERNAL_ERROR, "MKT_INFRA_004", "시세 저장 중 오류가 발생했습니다."),
    STOCK_MST_IMPORT_FAILED(ErrorType.INTERNAL_ERROR, "MKT_INFRA_005", "종목 마스터 파일 import 중 오류가 발생했습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    MarketInfraErrorCode(ErrorType errorType, String code, String message) {
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
