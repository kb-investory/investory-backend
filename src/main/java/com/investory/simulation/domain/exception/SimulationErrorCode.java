package com.investory.simulation.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum SimulationErrorCode implements ErrorCode {
    NON_EXECUTABLE_PRINCIPLE(ErrorType.INVALID_INPUT, "SIM_001", "실행할 수 없는 원칙입니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    SimulationErrorCode(ErrorType errorType, String code, String message) {
        this.errorType = errorType;
        this.code = code;
        this.message = message;
    }

    @Override
    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
