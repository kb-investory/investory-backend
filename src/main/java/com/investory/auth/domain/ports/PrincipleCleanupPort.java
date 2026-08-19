package com.investory.auth.domain.ports;

// principle.domain.services.PrincipleService.deleteAllPrincipleSets(Long)로 위임 예정.
// 계정 탈퇴 시 사용자의 투자원칙(principle_sets)을 전부 지운다.
public interface PrincipleCleanupPort {
    void deleteAllPrincipleSets(Long userId);
}
