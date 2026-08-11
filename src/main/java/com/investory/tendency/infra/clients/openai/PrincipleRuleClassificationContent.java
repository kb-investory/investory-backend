package com.investory.tendency.infra.clients.openai;

import lombok.Data;

import java.math.BigDecimal;

// LLM 응답 message.content(JSON 문자열): {"type":"STOP_LOSS","thresholdPercent":10}
// type이 STOP_LOSS/TAKE_PROFIT이 아니면 thresholdPercent는 null이어도 된다(ABSTRACT/EXCLUDED).
@Data
public class PrincipleRuleClassificationContent {
    private String type;
    private BigDecimal thresholdPercent;
}
