package com.investory.market.infra.exception;

import com.kbinvestory.backend.core.exception.InfraException;

public class MarketInfraException extends InfraException {
    public MarketInfraException(MarketInfraErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
