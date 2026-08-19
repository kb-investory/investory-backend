package com.investory.principle.infra.port_impls;

import com.investory.auth.domain.ports.PrincipleCleanupPort;
import com.investory.principle.domain.services.PrincipleService;
import org.springframework.stereotype.Component;

// auth.domain.ports를 참조하는 유일한 지점 — 받는 즉시 principle 자신의 서비스 호출로 위임한다(§5).
@Component
public class PrincipleCleanupPortImpl implements PrincipleCleanupPort {

    private final PrincipleService principleService;

    public PrincipleCleanupPortImpl(PrincipleService principleService) {
        this.principleService = principleService;
    }

    @Override
    public void deleteAllPrincipleSets(Long userId) {
        principleService.deleteAllPrincipleSets(userId);
    }
}
