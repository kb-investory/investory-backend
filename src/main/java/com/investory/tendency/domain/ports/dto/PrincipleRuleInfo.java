package com.investory.tendency.domain.ports.dto;

public record PrincipleRuleInfo(
    Long principleItemId,
    String principleText,
    String ruleJson   // nullable — 없거나 형식이 안 맞을 수 있음, 파싱은 호출측(tendency) 책임
) {
}
