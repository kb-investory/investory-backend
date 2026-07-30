package com.investory.market.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class MarketException extends BusinessException {

    public MarketException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MarketException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
