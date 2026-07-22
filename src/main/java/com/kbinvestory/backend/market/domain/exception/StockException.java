package com.kbinvestory.backend.market.domain.exception;

import com.kbinvestory.backend.core.exception.BusinessException;
import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.FieldError;

import java.util.List;

public class StockException extends BusinessException {

    public StockException(ErrorCode errorCode) {
        super(errorCode);
    }

    public StockException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}