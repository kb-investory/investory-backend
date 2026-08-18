package com.investory.market.infra.clients.kis;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class KisDisabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !context.getEnvironment().getProperty("kis.enabled", Boolean.class, true);
    }
}
