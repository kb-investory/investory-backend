package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.PrincipleRuleInfo;

import java.util.List;

public interface PrinciplePort {

    // 유저의 활성 원칙 세트 항목들. 원칙이 아예 없으면 빈 리스트(예외 아님) —
    // principle.PrincipleService.getActivePrincipleRules()의 계약을 그대로 이어받는다.
    List<PrincipleRuleInfo> findActivePrincipleRules(Long userId);
}
