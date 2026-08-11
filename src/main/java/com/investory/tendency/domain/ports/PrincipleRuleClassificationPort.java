package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.ports.dto.PrincipleRuleClassification;

public interface PrincipleRuleClassificationPort {
    PrincipleRuleClassification classify(String principleText);
}
