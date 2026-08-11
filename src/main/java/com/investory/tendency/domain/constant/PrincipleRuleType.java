package com.investory.tendency.domain.constant;

// 6번(원칙 이행 성향) 원칙 1건의 검증 경로 분류. journal.RationaleLabelType(매수 근거 분류, 별개 기능)과
// 무관 — 이름·값 모두 겹치지 않게 새로 정의한다.
public enum PrincipleRuleType {
    STOP_LOSS,    // 손절 규칙 — 수치 임계값(%) 존재, 일별 손익 데이터로 직접 검증 가능
    TAKE_PROFIT,  // 익절 규칙 — 수치 임계값(%) 존재, 일별 손익 데이터로 직접 검증 가능
    ABSTRACT,     // 정성적 규칙 — 수치 임계값 없음, LLM 채점(PrincipleComplianceGrade)으로 판정
    EXCLUDED      // 검증 불가 — 매수 트리거형("N% 오르면 매수") 등, "안 산 이유"는 데이터로 구분 불가
}
