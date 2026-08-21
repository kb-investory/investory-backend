package com.investory.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.core.exception.ErrorCode;
import com.investory.global.error.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

// JwtAuthenticationFilter는 여기까지 도달하기 전에 이미 AuthException을 잡아 SecurityContext만
// 비워두므로(§JwtAuthenticationFilter 주석 참고), GlobalExceptionHandler(@RestControllerAdvice)는
// 이 경로를 못 거친다 — 서블릿 필터 단계라 예외가 DispatcherServlet까지 전파되지 않기 때문이다.
// 그래서 같은 ErrorResponse 포맷을 여기서 직접 만들어 응답에 쓴다.
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        // JwtAuthenticationFilter가 AuthException을 잡았을 때만 이 attribute가 채워진다 — 토큰
        // 자체가 없었던 경우(그 필터의 try 블록에 아예 안 들어감)는 INVALID_TOKEN을 기본값으로 쓴다.
        Object attribute = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE);
        ErrorCode errorCode = attribute instanceof ErrorCode code ? code : AuthErrorCode.INVALID_TOKEN;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode, List.of()));
    }
}
