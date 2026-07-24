package com.kbinvestory.backend.account.domain.exception;

import com.kbinvestory.backend.core.exception.BusinessException;
import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.FieldError;

import java.util.List;

public class AccountConnectionException extends BusinessException {

    public AccountConnectionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AccountConnectionException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
