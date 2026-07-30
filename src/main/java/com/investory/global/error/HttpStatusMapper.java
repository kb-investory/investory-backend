package com.investory.global.error;

import org.springframework.http.HttpStatus;

import com.investory.core.exception.ErrorType;

public class HttpStatusMapper {
    public static HttpStatus toHttpStatus(ErrorType type) {
        return switch (type) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case EXTERNAL_ERROR -> HttpStatus.BAD_GATEWAY;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}