package com.investory.tendency.domain.constant;

// 6번(원칙 이행 성향) 최종 라벨. 수치형(STOP_LOSS/TAKE_PROFIT) 기회·이행 수와 추상형(ABSTRACT)
// grade 환산 점수를 기회 합산 기준으로 종합한 준수율로 결정된다.
public enum PrincipleAdherenceType {
    PRINCIPLE_ALIGNED,      // 원칙일치형 — 준수율 80% 이상
    SELECTIVE_COMPLIANCE,   // 선택적준수형 — 40% 이상 80% 미만
    REPEATED_DEVIATION,     // 반복이탈형 — 40% 미만
    INDETERMINATE           // 판정불가형 — 검증 가능한 기회(opportunity)가 0건(원칙 없음 포함)
}
