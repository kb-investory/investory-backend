package com.investory.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(
        basePackages = "com.investory",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SwaggerConfig.class)
        }
)
public class RootConfig {
        // KIS, mock broker, OAuth 3사, LLM(journal 근거 라벨링·tendency 원칙이행성향)이 전부 이 빈을
        // 공유한다. 예전엔 타임아웃이 전혀 없어 외부 호출 하나가 응답을 무한정 기다리게 할 수 있었다
        // (#196 — LLM이 느려지면 journal 저장 요청이 그대로 걸림). connect/read 타임아웃을 걸어
        // 어떤 외부 연동이 느려지거나 멈춰도 요청 스레드가 무한정 붙잡히지 않게 한다.
        @Bean
        public RestTemplate restTemplate() {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(3_000);
                requestFactory.setReadTimeout(15_000);
                return new RestTemplate(requestFactory);
        }
}
