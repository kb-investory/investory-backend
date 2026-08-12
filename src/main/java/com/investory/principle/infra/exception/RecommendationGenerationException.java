package com.investory.principle.infra.exception;

import com.investory.core.exception.ErrorType;
import com.investory.core.exception.InfraException;

// PrincipleInfraException(ErrorType.INTERNAL_ERROR)과 별도로 둔다 — LLM 추천 생성 실패는
// 내부 로직 오류가 아니라 외부 시스템 연동 실패라 ErrorType.EXTERNAL_ERROR가 맞다
// (tendency의 PrincipleAdherenceLlmException, journal의 RationaleLabelingException과 동일 이유).
public class RecommendationGenerationException extends InfraException {

    public RecommendationGenerationException(Throwable cause) {
        super(ErrorType.EXTERNAL_ERROR, "추천 생성 중 오류가 발생했습니다.", cause);
    }
}
