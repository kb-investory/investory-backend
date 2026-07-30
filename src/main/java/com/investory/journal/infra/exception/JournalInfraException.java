package com.investory.journal.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class JournalInfraException extends InfraException {

    public JournalInfraException(Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, "투자일지 목록을 조회하는 중 오류가 발생했습니다.", cause);
    }

    public JournalInfraException(String debugMessage, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, debugMessage, cause);
    }
}
