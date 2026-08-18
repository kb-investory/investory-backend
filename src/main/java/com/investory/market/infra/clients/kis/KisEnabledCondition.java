package com.investory.market.infra.clients.kis;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

// 이 프로젝트는 Spring Boot가 아니라 @ConditionalOnProperty(spring-boot-autoconfigure 소속)를 쓸 수 없어서,
// 순수 Spring 코어의 @Conditional로 같은 동작(프로퍼티 값에 따라 빈 활성화)을 직접 구현한다.
public class KisEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty("kis.enabled", Boolean.class, true);
    }
}
