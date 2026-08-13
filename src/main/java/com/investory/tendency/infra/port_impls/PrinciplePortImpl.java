package com.investory.tendency.infra.port_impls;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.tendency.domain.ports.PrinciplePort;
import com.investory.tendency.domain.ports.dto.PrincipleRuleInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// HoldingSummaryPortImpl/TradeLedgerPortImpl과 동일한 패턴 — 상대 도메인(principle) 서비스를
// 생성자로 직접 주입받아 그 결과를 tendency 로컬 DTO로 매핑한다.
@Component
public class PrinciplePortImpl implements PrinciplePort {

    private final PrincipleService principleService;

    public PrinciplePortImpl(PrincipleService principleService) {
        this.principleService = principleService;
    }

    @Override
    public List<PrincipleRuleInfo> findActivePrincipleRules(Long userId) {
        return principleService.getActivePrincipleRules(userId).stream()
                .map(r -> new PrincipleRuleInfo(r.principleSetItemId(), r.principleText(), r.ruleJson()))
                .collect(Collectors.toList());
    }
}
