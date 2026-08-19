package com.investory.auth.infra.clients;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.model.OAuthUserInfo;

public class FakeOAuthClient implements OAuthClient {

    private final OAuthProviderType provider;
    private OAuthUserInfo nextUserInfo;

    public FakeOAuthClient(OAuthProviderType provider) {
        this.provider = provider;
    }

    public void willReturnUserInfo(OAuthUserInfo userInfo) {
        this.nextUserInfo = userInfo;
    }

    @Override
    public OAuthProviderType getProvider() {
        return provider;
    }

    @Override
    public String getAuthorizeUrl(String state) {
        return "https://fake-oauth/authorize";
    }

    @Override
    public String getAccessToken(String code, String state) {
        return "fake-access-token";
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        return nextUserInfo;
    }
}
