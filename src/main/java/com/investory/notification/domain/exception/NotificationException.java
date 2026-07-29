package com.investory.notification.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class NotificationException extends BusinessException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
