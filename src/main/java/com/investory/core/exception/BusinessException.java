package com.investory.core.exception;

import java.util.List;

public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<FieldError> fieldErrors;   // 없으면 빈 리스트

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
    }

    protected BusinessException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
}