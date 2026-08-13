package com.investory.tendency.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class TendencyInfraException extends InfraException {

    public TendencyInfraException(Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, "투자 성향 데이터를 조회하는 중 오류가 발생했습니다.", cause);
    }

    public TendencyInfraException(String debugMessage, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, debugMessage, cause);
    }
}
