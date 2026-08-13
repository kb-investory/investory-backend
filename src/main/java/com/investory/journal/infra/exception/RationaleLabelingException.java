package com.investory.journal.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class RationaleLabelingException extends InfraException {

    public RationaleLabelingException(Throwable cause) {
        super(ErrorType.EXTERNAL_ERROR, "매수 판단 근거 라벨링 중 오류가 발생했습니다.", cause);
    }
}
