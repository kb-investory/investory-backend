package com.investory.global.web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(
        basePackages = "com.investory",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class)
)
public class WebConfig {
}
