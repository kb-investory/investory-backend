package com.investory.principle.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class PrincipleInfraException extends InfraException {

    public PrincipleInfraException(String debugMessage, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, debugMessage, cause);
    }
}
