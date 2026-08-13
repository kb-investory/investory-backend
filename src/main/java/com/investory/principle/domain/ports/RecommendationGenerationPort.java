package com.investory.principle.domain.ports;

import com.investory.principle.domain.ports.dto.GeneratedRecommendation;

import java.util.List;

public interface RecommendationGenerationPort {

    // 성향 분석 유형(코드+사람이 읽을 수 있는 이름)에 맞는 추천 원칙 후보들을 생성한다.
    // analysis_types 테이블을 직접 조회하지 않고, 호출측이 이벤트로 이미 전달받은 코드/이름을 그대로 넘긴다.
    List<GeneratedRecommendation> generate(String analysisTypeCode, String analysisTypeName);
}
