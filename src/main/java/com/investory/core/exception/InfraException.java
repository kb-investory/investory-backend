package com.investory.core.exception;

public abstract class InfraException extends RuntimeException {
    private final ErrorType type;

    protected InfraException(ErrorType type, String debugMessage, Throwable cause) {
        super(debugMessage, cause);   // 로그 전용 — 응답 바디에는 절대 노출 안 됨
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}
