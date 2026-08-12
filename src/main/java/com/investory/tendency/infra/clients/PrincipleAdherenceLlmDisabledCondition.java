package com.investory.tendency.infra.clients;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class PrincipleAdherenceLlmDisabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !context.getEnvironment().getProperty("llm.enabled", Boolean.class, true);
    }
}
