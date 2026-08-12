package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.PrincipleRuleInfo;

import java.util.ArrayList;
import java.util.List;

public class FakePrinciplePort implements PrinciplePort {

    private final List<PrincipleRuleInfo> rules = new ArrayList<>();

    public void add(PrincipleRuleInfo... rules) {
        this.rules.addAll(List.of(rules));
    }

    @Override
    public List<PrincipleRuleInfo> findActivePrincipleRules(Long userId) {
        return List.copyOf(rules);
    }
}
