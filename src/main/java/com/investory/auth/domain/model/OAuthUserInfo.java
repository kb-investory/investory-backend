package com.investory.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

// OAuth 인증 후 각 Provider로부터 정보를 가져오는 Object
@Getter
@Builder
public class OAuthUserInfo {
    private String oauthSubId;
    private String email;
    private String nickname;
}