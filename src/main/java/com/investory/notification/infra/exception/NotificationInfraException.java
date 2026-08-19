package com.investory.notification.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class NotificationInfraException extends InfraException {
    public NotificationInfraException(String debugMessage, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, debugMessage, cause);
    }
}
