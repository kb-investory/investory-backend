package com.investory.core.exception;

public enum ErrorType {
    NOT_FOUND,       // 대상이 존재하지 않음
    INVALID_INPUT,   // 요청/입력이 유효하지 않음
    CONFLICT,        // 현재 상태와 충돌
    UNAUTHORIZED,    // 인증 안 됨
    FORBIDDEN,       // 권한 없음
    UNPROCESSABLE,   // 입력 형식은 유효하나 도메인 조건을 충족하지 못함
    EXTERNAL_ERROR,  // 외부 시스템 연동 실패
    INTERNAL_ERROR   // 그 외 예상 못한 시스템 오류
}