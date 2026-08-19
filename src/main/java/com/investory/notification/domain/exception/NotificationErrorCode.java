package com.investory.notification.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(ErrorType.NOT_FOUND, "NOTI_001", "존재하지 않는 알림입니다."),
    INVALID_SETTINGS_DATA(ErrorType.INVALID_INPUT, "NOTI_002", "알림 수신 설정 값이 올바르지 않습니다."),
    INVALID_NOTIFICATION_DATA(ErrorType.INVALID_INPUT, "NOTI_003", "알림 데이터가 올바르지 않습니다."),
    INVALID_PAGE_PARAMS(ErrorType.INVALID_INPUT, "NOTI_004", "페이지 조회 조건이 올바르지 않습니다.");

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
