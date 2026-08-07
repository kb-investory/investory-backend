package com.investory.auth.infra.clients;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.OAuthUserInfo;

public interface OAuthClient {
    OAuthProviderType getProvider();

    // state: CSRF 방지용 임의 문자열. 네이버는 필수로 사용하고, 카카오/구글 구현체는 무시한다.
    String getAuthorizeUrl(String state);

    // state: getAuthorizeUrl에서 쓴 값과 동일한 값을 그대로 넘겨줘야 한다(네이버는 토큰 교환 시 이 값을 검증함).
    String getAccessToken(String code, String state);

    OAuthUserInfo getUserInfo(String accessToken);
}
