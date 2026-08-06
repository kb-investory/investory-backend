package com.investory.market.domain.exception;


import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;

public class MarketException extends BusinessException {
    public MarketException(ErrorCode errorCode) {
        super(errorCode);
    }
}
