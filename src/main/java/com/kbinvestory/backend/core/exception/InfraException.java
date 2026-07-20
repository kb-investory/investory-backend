package com.kbinvestory.backend.core.exception;

public abstract class InfraException extends RuntimeException {
    private final ErrorCode errorCode;

    protected InfraException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);   // cause를 반드시 받아서 로그에 원인이 남게 함
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}