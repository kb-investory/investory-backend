package com.investory.global.error;

import com.investory.core.exception.BusinessException;
import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = HttpStatusMapper.toHttpStatus(errorCode.getErrorType());
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(errorCode, e.getFieldErrors()));
    }

    @ExceptionHandler(InfraException.class)
    public ResponseEntity<ErrorResponse> handleInfraException(InfraException e) {
        // 주의: cause의 상세 메시지는 응답에 절대 포함시키지 않음 (로그에만 남김)
        log.error("[{}] {}", e.getType(), e.getMessage(), e.getCause());
        HttpStatus status = HttpStatusMapper.toHttpStatus(e.getType());
        return ResponseEntity.status(status)
                .body(ErrorResponse.generic(e.getType()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류", e);
        HttpStatus status = HttpStatusMapper.toHttpStatus(ErrorType.INTERNAL_ERROR);
        return ResponseEntity.status(status)
                .body(ErrorResponse.generic(ErrorType.INTERNAL_ERROR));
    }

}
