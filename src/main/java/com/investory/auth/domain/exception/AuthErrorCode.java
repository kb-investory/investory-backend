package com.investory.auth.domain.exception;

import com.investory.core.exception.ErrorCode;
import com.investory.core.exception.ErrorType;

public enum AuthErrorCode implements ErrorCode {
    UNSUPPORTED_PROVIDER(ErrorType.INVALID_INPUT,  "AUTH_001","지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_REQUEST(ErrorType.INVALID_INPUT,  "AUTH_002","요청 값이 올바르지 않습니다."),
    OAUTH_STATE_MISMATCH(ErrorType.INVALID_INPUT,  "AUTH_003","OAuth state 값이 일치하지 않습니다. 인가 요청을 다시 시도해주세요."),
    OAUTH_AUTHENTICATION_FAILED(ErrorType.EXTERNAL_ERROR,  "AUTH_004","소셜 로그인 인증에 실패했습니다."),
    WITHDRAWN_USER(ErrorType.FORBIDDEN,  "AUTH_005","탈퇴한 회원입니다."),
    USER_NOT_FOUND(ErrorType.NOT_FOUND,  "AUTH_006","존재하지 않는 회원입니다."),
    INVALID_TOKEN(ErrorType.UNAUTHORIZED,  "AUTH_007","유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(ErrorType.UNAUTHORIZED,  "AUTH_008","만료된 토큰입니다."),
    NOT_REFRESH_TOKEN(ErrorType.UNAUTHORIZED,  "AUTH_009","리프레시 토큰이 아닙니다.");

    private final ErrorType errorType;
    private final String code;
    private final String message;

    AuthErrorCode(ErrorType errorType, String code, String message) {
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
