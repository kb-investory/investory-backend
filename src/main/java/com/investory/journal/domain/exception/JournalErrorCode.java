package com.investory.journal.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum JournalErrorCode implements ErrorCode {
    JOURNAL_ALREADY_EXISTS(ErrorType.CONFLICT, "JNL_001", "이미 작성된 일지가 존재합니다."),
    JOURNAL_NOT_EDITABLE(ErrorType.CONFLICT, "JNL_002", "수정할 수 없는 일지입니다."),
    TRADE_DATE_MISMATCH(ErrorType.INVALID_INPUT, "JNL_003", "거래 일자와 일지 날짜가 일치하지 않습니다."),
    DUPLICATE_TRADE_ID(ErrorType.INVALID_INPUT, "JNL_004", "이미 등록된 거래 근거입니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    JournalErrorCode(ErrorType errorType, String code, String message) {
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
