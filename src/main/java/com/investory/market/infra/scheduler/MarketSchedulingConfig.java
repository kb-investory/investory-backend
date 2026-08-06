package com.investory.market.infra.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 어노테이션이 동작하려면 컨텍스트 안에 @EnableScheduling이 한 번은 선언돼 있어야 한다.
 * market 패키지 밖의 기존 설정 클래스를 건드리지 않기 위해 이 패키지 안에 별도로 둔다.
 */
@Configuration
@EnableScheduling
public class MarketSchedulingConfig {
}
