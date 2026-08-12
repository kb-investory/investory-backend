package com.investory.principle.domain.services.dto.result;

// getActivePrincipleSet()의 PrincipleItemResult와 달리 ruleJson을 포함한다 — 6번(원칙 이행 성향)이
// 규칙을 파싱해 검증 경로를 정하는 데 필요하기 때문. REST 응답(PrincipleItemResponse)에는 노출되지 않는다.
public record PrincipleRuleItemResult(
    Long principleSetItemId,
    String principleText,
    String ruleJson
) {
}
