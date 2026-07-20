package com.kbinvestory.backend.core.exception;

// 각 기능 패키지에서 enum으로 ErrorCode 구현해서 사용
public interface ErrorCode {
    ErrorType getErrorType();
    String getCode();
    String getMessage();
}
