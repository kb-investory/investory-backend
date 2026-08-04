package com.investory.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableSwagger2WebMvc
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = "springfox.documentation.swagger.web")
public class SwaggerConfig implements WebMvcConfigurer {
    @Bean
    public Docket api(@Value("${app.uri.prefix}") String pathMapping) {
        return new Docket(DocumentationType.SWAGGER_2)
                .pathMapping(pathMapping)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.investory"))
                .paths(PathSelectors.any())
                .build();
    }

    // springfox-swagger-ui 3.0.0은 정적 리소스만 담긴 webjar라, Boot의 기본 정적 리소스 서빙에 의존하는 대신
    // 여기서 직접 리소스 핸들러를 등록해야 /swagger-ui/** 가 서빙된다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
    }
}