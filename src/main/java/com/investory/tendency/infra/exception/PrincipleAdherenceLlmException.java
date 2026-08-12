package com.investory.tendency.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

public class PrincipleAdherenceLlmException extends InfraException {

    public PrincipleAdherenceLlmException(Throwable cause) {
        super(ErrorType.EXTERNAL_ERROR, "원칙 이행 성향 LLM 판정 중 오류가 발생했습니다.", cause);
    }
}
