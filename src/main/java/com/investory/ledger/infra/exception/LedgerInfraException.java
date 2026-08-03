package com.investory.ledger.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class LedgerInfraException extends InfraException {

    public LedgerInfraException(Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, "거래원장 데이터를 처리하는 중 오류가 발생했습니다.", cause);
    }

    public LedgerInfraException(String debugMessage, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, debugMessage, cause);
    }
}
