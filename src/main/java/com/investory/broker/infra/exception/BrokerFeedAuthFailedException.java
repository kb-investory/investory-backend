package com.investory.broker.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

// 목 증권사 인증(401)을 domain이 잡아 BrokerException(BROKER_AUTH_FAILED)으로 변환할 수 있게
// 구분해서 던지는 캐치 대상 예외. tendency의 RationaleTaggingException과 동일한 용도.
public class BrokerFeedAuthFailedException extends InfraException {
    public BrokerFeedAuthFailedException(Throwable cause) {
        super(ErrorType.EXTERNAL_ERROR, "목 증권사 인증에 실패했습니다.", cause);
    }
}
