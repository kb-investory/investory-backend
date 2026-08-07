package com.investory.broker.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class BrokerInfraException extends InfraException {

    public BrokerInfraException(Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, "증권사 목록을 조회하는 중 오류가 발생했습니다.", cause);
    }

    public BrokerInfraException(ErrorType type, String debugMessage, Throwable cause) {
        super(type, debugMessage, cause);
    }
}