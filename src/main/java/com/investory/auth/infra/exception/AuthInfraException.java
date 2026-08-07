package com.investory.auth.infra.exception;

import com.investory.core.exception.InfraException;

public class AuthInfraException extends InfraException {

    public AuthInfraException(AuthInfraErrorCode errorCode, Throwable cause) {
        super(errorCode.getErrorType(), errorCode.getMessage(), cause);
    }
}