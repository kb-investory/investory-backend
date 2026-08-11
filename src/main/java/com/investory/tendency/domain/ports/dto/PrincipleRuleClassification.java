package com.investory.tendency.domain.ports.dto;

import com.investory.tendency.domain.constant.PrincipleRuleType;

import java.math.BigDecimal;

// thresholdPercent는 STOP_LOSS/TAKE_PROFIT일 때만 non-null.
public record PrincipleRuleClassification(PrincipleRuleType type, BigDecimal thresholdPercent) {
}
