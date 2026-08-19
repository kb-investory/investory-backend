package com.investory.broker.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class BrokerInfraException extends InfraException {

    public BrokerInfraException(ErrorType type, String debugMessage, Throwable cause) {
        super(type, debugMessage, cause);
    }
}