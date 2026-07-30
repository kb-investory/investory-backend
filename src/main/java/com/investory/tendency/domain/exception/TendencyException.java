package com.investory.tendency.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class TendencyException extends BusinessException {

    public TendencyException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TendencyException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
