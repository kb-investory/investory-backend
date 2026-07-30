package com.investory.simulation.domain.exception;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.FieldError;

import java.util.List;

public class SimulationException extends BusinessException {

    public SimulationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SimulationException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, fieldErrors);
    }
}
