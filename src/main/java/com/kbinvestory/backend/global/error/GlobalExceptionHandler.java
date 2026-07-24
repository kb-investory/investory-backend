package com.kbinvestory.backend.global.error;

import com.kbinvestory.backend.core.exception.BusinessException;
import com.kbinvestory.backend.core.exception.ErrorCode;
import com.kbinvestory.backend.core.exception.InfraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = HttpStatusMapper.toHttpStatus(errorCode.getErrorType());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(errorCode.getCode(), e.getMessage(), e.getFieldErrors()));
    }

    @ExceptionHandler(InfraException.class)
    public ResponseEntity<ErrorResponse> handleInfraException(InfraException e) {
        log.error("[{}] {}", e.getErrorCode().getCode(), e.getMessage(), e.getCause());
        HttpStatus status = HttpStatusMapper.toHttpStatus(e.getErrorCode().getErrorType());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage(), List.of()));
        // 주의: e.getCause()의 상세 메시지는 응답에 절대 포함시키지 않음 (로그에만 남김)
    }

}
