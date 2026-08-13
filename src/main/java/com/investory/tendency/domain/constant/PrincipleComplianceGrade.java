package com.investory.tendency.domain.constant;

// ABSTRACT 원칙 1건에 대한 LLM 채점 결과(3단계).
public enum PrincipleComplianceGrade {
    FOLLOWED,  // 지킴
    PARTIAL,   // 부분적
    VIOLATED   // 어김
}
