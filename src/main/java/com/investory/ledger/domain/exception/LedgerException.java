package com.investory.ledger.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class LedgerException extends BusinessException {

    public LedgerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public LedgerException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
