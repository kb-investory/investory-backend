package com.investory.auth.infra.clients.google;

import lombok.Data;

@Data
public class GoogleTokenResponse {
    private String access_token;
    private String refresh_token;
    private String token_type;
    private String state;
    private int expires_in;
    // 토큰 교환 실패 시 HTTP 200으로 이 두 필드만 채워 응답하는 경우를 대비해 로그에 보이게 한다.
    private String error;
    private String error_description;
}
