package com.investory.principle.domain.ports.dto;

// LLM이 특정 성향(analysis type)에 맞춰 생성한 추천 원칙 후보 1건.
public record GeneratedRecommendation(
    String text,
    String reason,
    String ruleJson   // nullable — 정성적 추천이면 수치 규칙이 없을 수 있음
) {
}
