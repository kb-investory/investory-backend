package com.investory.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;
import com.investory.core.exception.FieldError;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> fieldErrors
) {

    // BusinessException용 — 도메인 코드/메시지를 그대로 노출
    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), Instant.now(), fieldErrors);
    }

    // InfraException / 미처리 예외용 — 도메인 구분 없이 고정 문구 2종 중 하나
    public static ErrorResponse generic(ErrorType type) {
        String message = switch (type) {
            case EXTERNAL_ERROR -> "외부 서비스 연동 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            default -> "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        };
        return new ErrorResponse(type.name(), message, Instant.now(), List.of());
    }
}
