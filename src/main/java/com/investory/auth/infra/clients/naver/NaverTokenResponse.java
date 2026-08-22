package com.investory.auth.infra.clients.naver;

import lombok.Data;

@Data
public class NaverTokenResponse {
    private String access_token;
    private String refresh_token;
    private String token_type;
    private String state;
    private int expires_in;
    // 네이버가 토큰 교환에 실패해도 HTTP 200으로 이 두 필드만 채워 응답하는 경우가 있어,
    // 이 필드들이 없으면 access_token이 null인 이유를 로그로 알 수가 없다.
    private String error;
    private String error_description;
}
