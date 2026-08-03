package com.investory.auth.domain.services.dto.command;

import com.investory.auth.domain.constant.OAuthProviderType;

// state: 인가 요청 시 발급한 CSRF 방지용 값. 네이버는 토큰 교환에 필수로 쓰이고, 나머지 provider는 null이어도 된다.
public record OAuthLoginCommand(OAuthProviderType oauthProvider, String code, String state) {
}
