package com.investory.broker.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class BrokerException extends BusinessException {

    public BrokerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BrokerException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
