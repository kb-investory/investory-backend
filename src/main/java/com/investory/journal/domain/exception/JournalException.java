package com.investory.journal.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class JournalException extends BusinessException {

    public JournalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public JournalException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
