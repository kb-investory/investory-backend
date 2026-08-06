package com.investory.market.infra.exception;


import com.investory.core.exception.InfraException;

public class MarketInfraException extends InfraException {
    public MarketInfraException(MarketInfraErrorCode errorCode, Throwable cause) {
        super(errorCode.getErrorType(), errorCode.getMessage(), cause);
    }
}
