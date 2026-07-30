package com.investory.principle.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class PrincipleException extends BusinessException {

    public PrincipleException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PrincipleException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
