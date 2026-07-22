package com.kbinvestory.backend.global.initializer;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

// Root config(RootConfig)는 AppInitializer(AbstractAnnotationConfigDispatcherServletInitializer)가 이미 등록하므로,
// 여기서는 springSecurityFilterChain 필터만 추가로 등록한다.
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
}