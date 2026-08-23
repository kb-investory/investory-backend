package com.investory.global.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 배치서버 분리 전/후 API 응답지연 비교용. REQ 접두어로 grep해서 durationMs를 뽑아,
// SYNC_BATCH(BrokerAccountSyncScheduler) 실행 구간과 겹치는 시간대의 p95를 비교한다.
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = "requestStartTimeMs";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute(START_TIME_ATTRIBUTE);
        log.info("REQ method={} path={} status={} durationMs={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(),
                System.currentTimeMillis() - startTime);
    }
}
