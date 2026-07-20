package com.kbinvestory.backend.global.error;

import com.kbinvestory.backend.core.exception.BusinessException;
import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.InfraException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = HttpStatusMapper.toHttpStatus(errorCode.getErrorType());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(errorCode.getCode(), e.getMessage(), e.getFieldErrors()));
    }

    @ExceptionHandler(InfraException.class)
    public ResponseEntity<ErrorResponse> handleInfraException(InfraException e) {
        // 로깅 필요
        HttpStatus status = HttpStatusMapper.toHttpStatus(e.getErrorCode().getErrorType());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage(), List.of()));
        // 주의: e.getCause()의 상세 메시지는 응답에 절대 포함시키지 않음
    }

}
