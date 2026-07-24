package com.kbinvestory.backend.account.infra.exception;

import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.InfraException;

public class AccountInfraException extends InfraException {

    public AccountInfraException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}