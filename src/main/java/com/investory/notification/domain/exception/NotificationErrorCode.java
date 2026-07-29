package com.investory.notification.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

// TODO: 도메인 규칙이 확정되는 대로 실제 코드로 교체/보강 (CLAUDE.md §10에 아직 미확정 상태로 표기됨)
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(ErrorType.NOT_FOUND, "NOTI_001", "존재하지 않는 알림입니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    NotificationErrorCode(ErrorType errorType, String code, String message) {
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
