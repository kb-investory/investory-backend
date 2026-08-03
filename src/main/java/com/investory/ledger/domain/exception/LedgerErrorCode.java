package com.investory.ledger.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

// code 값은 API 명세의 에러 응답 code 문자열과 동일하게 맞춤 (예: "LEDGER_INVALID_DATE_RANGE").
// 응답 바디의 실제 필드명(errorCode/message/timestamp/fieldErrors)은 global/error/ErrorResponse가
// 결정하며 지금 명세 문서(code/message 2개 필드)와는 다르다 — ledger가 통제 가능한 범위 밖이라 이 부분은
// 그대로 두고 code 값만 명세와 맞춘다.
public enum LedgerErrorCode implements ErrorCode {
    TRADE_NOT_FOUND(ErrorType.NOT_FOUND, "TRADE_NOT_FOUND", "거래내역을 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(ErrorType.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다."),
    LEDGER_INVALID_DATE_RANGE(ErrorType.INVALID_INPUT, "LEDGER_INVALID_DATE_RANGE", "조회 시작일은 종료일보다 늦을 수 없습니다."),
    LEDGER_INVALID_TRADE_SIDE(ErrorType.INVALID_INPUT, "LEDGER_INVALID_TRADE_SIDE", "매매 구분은 BUY 또는 SELL이어야 합니다."),
    LEDGER_INVALID_PAGE_REQUEST(ErrorType.INVALID_INPUT, "LEDGER_INVALID_PAGE_REQUEST", "페이지 번호와 조회 개수를 확인해 주세요."),
    LEDGER_INVALID_TRADE_ID(ErrorType.INVALID_INPUT, "LEDGER_INVALID_TRADE_ID", "올바르지 않은 거래 ID입니다."),
    LEDGER_INVALID_ACCOUNT_ID(ErrorType.INVALID_INPUT, "LEDGER_INVALID_ACCOUNT_ID", "올바르지 않은 계좌 ID입니다."),
    LEDGER_INVALID_SECURITY_ID(ErrorType.INVALID_INPUT, "LEDGER_INVALID_SECURITY_ID", "올바르지 않은 종목 ID입니다."),

    // 명세에 없는 내부 불변식 검증용 (도메인 모델 생성 시 필수값 누락 방지)
    INVALID_TRADE_DATA(ErrorType.INVALID_INPUT, "LEDGER_INVALID_TRADE_DATA", "거래 데이터가 올바르지 않습니다."),
    INVALID_HOLDING_DATA(ErrorType.INVALID_INPUT, "LEDGER_INVALID_HOLDING_DATA", "보유현황 데이터가 올바르지 않습니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    LedgerErrorCode(ErrorType errorType, String code, String message) {
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
