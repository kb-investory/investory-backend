package com.investory.auth.infra.clients.kakao;

import lombok.Data;

@Data
public class KakaoTokenResponse {
    private String access_token;
    private String refresh_token;
    private String token_type;
    private int expires_in;
}
